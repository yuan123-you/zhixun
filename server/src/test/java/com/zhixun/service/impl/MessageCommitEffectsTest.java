package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.AesUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.entity.User;
import com.zhixun.entity.UserMessage;
import com.zhixun.entity.UserSettings;
import com.zhixun.mapper.*;
import com.zhixun.service.AIService;
import com.zhixun.vo.AIResponseVO;
import com.zhixun.vo.MessageVO;
import com.zhixun.websocket.ChatWebSocketHandler;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.TextMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real H2/MyBatis writes and Spring REQUIRED transactions; no live dependencies. */
@SuppressWarnings("unchecked")
class MessageCommitEffectsTest {
    private DriverManagerDataSource source;
    private JdbcTemplate jdbc;
    private TransactionTemplate outer;
    private UserMessageMapper messages;
    private UserMapper users;
    private UserSettingsMapper settings;
    private UserFollowMapper follows;
    private AesUtil aes;
    private SensitiveWordUtil sensitive;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private RabbitTemplate broker;
    private AIService ai;
    private ObjectMapper json;
    private MessageServiceImpl target;
    private MessageServiceImpl service;
    private MessageSendRequest request;

    @BeforeEach void setUp() throws Exception {
        source = new DriverManagerDataSource("jdbc:h2:mem:message_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE user_message (id BIGINT AUTO_INCREMENT PRIMARY KEY, sender_id BIGINT NOT NULL, receiver_id BIGINT NOT NULL, type VARCHAR(32), content VARCHAR(4096), is_read INT, created_at TIMESTAMP)");
        var manager = new DataSourceTransactionManager(source);
        outer = new TransactionTemplate(manager);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(UserMessageMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source); factory.setConfiguration(configuration);
        messages = spy(new SqlSessionTemplate(factory.getObject()).getMapper(UserMessageMapper.class));
        users = mock(UserMapper.class); settings = mock(UserSettingsMapper.class); follows = mock(UserFollowMapper.class);
        User sender = new User(); sender.setId(7L); sender.setNickname("Sender"); sender.setAvatar("avatar");
        User receiver = new User(); receiver.setId(8L); receiver.setNickname("Receiver");
        when(users.selectById(7L)).thenReturn(sender); when(users.selectById(8L)).thenReturn(receiver);
        aes = mock(AesUtil.class); when(aes.encrypt(anyString())).thenAnswer(i -> "encrypted:" + i.getArgument(0));
        when(aes.decrypt(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).substring(10));
        sensitive = mock(SensitiveWordUtil.class);
        redis = mock(StringRedisTemplate.class); values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        broker = mock(RabbitTemplate.class); ai = mock(AIService.class); json = spy(new ObjectMapper());
        target = new MessageServiceImpl(messages, users, settings, follows, aes, sensitive, redis, broker, ai, json);
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        service = (MessageServiceImpl) proxy.getProxy();
        request = new MessageSendRequest(); request.setReceiverId(8L); request.setContent("hello");
    }

    @AfterEach void closeDatabase() { jdbc.execute("SHUTDOWN"); }
    private int committedRows() throws Exception {
        // Deliberately bypass Spring's thread-bound transaction connection.
        try (var connection = source.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM user_message")) {
            result.next(); return result.getInt(1);
        }
    }
    private int rows() { return jdbc.queryForObject("SELECT COUNT(*) FROM user_message", Integer.class); }
    private void noEffects() { verifyNoInteractions(redis, broker); }
    private MessageVO send() { return service.sendMessage(7L, request); }

    @Test void effectsWaitForOuterCommitAndObserveCommittedRow() {
        doAnswer(i -> { assertEquals(1, committedRows()); return null; }).when(broker).convertAndSend(anyString(), anyString(), any(Object.class));
        outer.executeWithoutResult(status -> {
            MessageVO vo = send(); assertNotNull(vo.getId()); assertNotNull(vo.getCreatedAt());
            assertEquals("hello", vo.getContent()); assertEquals(1, rows()); noEffects();
            assertEquals(0, assertDoesNotThrow(this::committedRows));
        });
        assertEquals(1, rows());
        verify(values).increment("message:unread:8:7");
        verify(redis).expire("message:unread:8:7", 7, TimeUnit.DAYS);
        verify(values).set("message:conversation:7:8", "hello", 7, TimeUnit.DAYS);
        verify(values).set("message:conversation:8:7", "hello", 7, TimeUnit.DAYS);
        verify(broker, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        assertEquals("encrypted:hello", jdbc.queryForObject("SELECT content FROM user_message", String.class));
    }

    @Test void outerRollbackDiscardsRowAndAllEffects() {
        outer.executeWithoutResult(status -> { send(); status.setRollbackOnly(); });
        assertEquals(0, rows()); noEffects();
    }

    @Test void thrownOuterFailureDiscardsEffects() {
        assertThrows(IllegalStateException.class, () -> outer.executeWithoutResult(status -> { send(); throw new IllegalStateException("outer failure"); }));
        assertEquals(0, rows()); noEffects();
    }

    @Test void standaloneProxyCommitsBeforePublishing() {
        MessageVO vo = send(); assertNotNull(vo.getCreatedAt()); assertEquals(1, rows());
        verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void unreadFailureDoesNotUndoRowOrSkipConversationAndNotification() {
        when(values.increment(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));
        assertDoesNotThrow(this::send); assertEquals(1, rows());
        verify(values).set("message:conversation:7:8", "hello", 7, TimeUnit.DAYS);
        verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void conversationFailureDoesNotSkipNotification() {
        doThrow(new IllegalStateException("cache unavailable")).when(values).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        assertDoesNotThrow(this::send); assertEquals(1, rows());
        verify(values).increment(anyString()); verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void mqFailureFallsBackOnlyAfterCommit() {
        doThrow(new IllegalStateException("MQ unavailable")).when(broker).convertAndSend(anyString(), anyString(), any(Object.class));
        try (MockedStatic<ChatWebSocketHandler> ws = mockStatic(ChatWebSocketHandler.class)) {
            outer.executeWithoutResult(status -> { send(); ws.verifyNoInteractions(); noEffects(); });
            ws.verify(() -> ChatWebSocketHandler.sendToUser(eq(8L), any(TextMessage.class)), times(1));
            assertEquals(1, rows());
        }
    }

    @Test void allExternalFailuresCannotTurnCommittedWriteIntoFailure() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("Redis down"));
        doThrow(new IllegalStateException("MQ down")).when(broker).convertAndSend(anyString(), anyString(), any(Object.class));
        try (MockedStatic<ChatWebSocketHandler> ws = mockStatic(ChatWebSocketHandler.class)) {
            ws.when(() -> ChatWebSocketHandler.sendToUser(anyLong(), any(TextMessage.class))).thenThrow(new IllegalStateException("WS down"));
            assertDoesNotThrow(this::send); assertEquals(1, rows());
            ws.verify(() -> ChatWebSocketHandler.sendToUser(eq(8L), any(TextMessage.class)));
        }
    }

    @ParameterizedTest @ValueSource(ints = {0, 2})
    void nonSingleRowInsertIsMandatoryFailure(int count) {
        doReturn(count).when(messages).insert(any(UserMessage.class));
        assertThrows(BusinessException.class, this::send); assertEquals(0, rows()); noEffects();
    }

    @Test void databaseExceptionHasNoEffects() {
        doThrow(new IllegalStateException("database rejected insert")).when(messages).insert(any(UserMessage.class));
        assertThrows(BusinessException.class, this::send); assertEquals(0, rows()); noEffects();
    }

    @Test void databaseConstraintFailureRollsBackWithoutEffects() {
        jdbc.execute("ALTER TABLE user_message ADD CONSTRAINT reject_hello CHECK (content <> 'encrypted:hello')");
        assertThrows(BusinessException.class, this::send); assertEquals(0, rows()); noEffects();
    }

    @Test void payloadIsDetachedFromResponseRequestAndUserMetadata() {
        outer.executeWithoutResult(status -> {
            MessageVO vo = send();
            request.setContent("changed request"); request.setReceiverId(99L);
            vo.setContent("changed response"); vo.setId(99L); vo.setCreatedAt(null);
            users.selectById(7L).setNickname("changed sender");
            clearInvocations(users); noEffects();
        });
        var payload = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(broker).convertAndSend(anyString(), anyString(), payload.capture());
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) payload.getValue()).get("data");
        assertEquals("hello", data.get("content")); assertEquals(8L, data.get("receiverId"));
        assertEquals("Sender", data.get("senderNickname")); assertNotNull(data.get("createdAt"));
        assertEquals("text", data.get("messageType")); assertEquals("CHAT", ((Map<?, ?>) payload.getValue()).get("type"));
        verifyNoInteractions(users);
    }

    @Test void nullableSenderMetadataStillPublishes() {
        users.selectById(7L).setNickname(null); users.selectById(7L).setAvatar(null);
        assertDoesNotThrow(this::send); verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void nonTextContentRemainsUnencrypted() {
        request.setType("image"); request.setContent("https://example.invalid/image.png");
        MessageVO vo = send(); assertEquals(request.getContent(), vo.getContent());
        assertEquals(request.getContent(), jdbc.queryForObject("SELECT content FROM user_message", String.class));
        verify(aes, never()).encrypt(anyString());
    }

    @Test void deniedPrivatePermissionDoesNotPersistOrPublish() {
        UserSettings policy = new UserSettings(); policy.setMessagePermission(1);
        when(settings.selectOne(any())).thenReturn(policy); when(follows.selectCount(any())).thenReturn(0L);
        assertThrows(BusinessException.class, this::send); assertEquals(0, rows()); noEffects();
    }

    @Test void selfMessageAndSensitiveContentRemainRejected() {
        request.setReceiverId(7L); assertThrows(BusinessException.class, this::send);
        request.setReceiverId(8L); when(sensitive.containsSensitiveWord("hello")).thenReturn(true);
        assertThrows(BusinessException.class, this::send); assertEquals(0, rows()); noEffects();
    }

    @Test void missingGeneratedIdIsMandatoryFailure() {
        doReturn(1).when(messages).insert(any(UserMessage.class));
        assertThrows(BusinessException.class, this::send); noEffects();
    }

    @Test void directNonTransactionalCallStillPublishesAfterInsert() {
        MessageVO vo = target.sendMessage(7L, request);
        assertNotNull(vo.getCreatedAt()); assertEquals(1, rows());
        verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void expiryFailureDoesNotSkipOtherEffects() {
        when(redis.expire(anyString(), anyLong(), any(TimeUnit.class))).thenThrow(new IllegalStateException("expiry failed"));
        assertDoesNotThrow(this::send); assertEquals(1, rows());
        verify(values).set("message:conversation:7:8", "hello", 7, TimeUnit.DAYS);
        verify(broker).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test void serializationFailureDoesNotUndoCommittedWrite() throws Exception {
        doThrow(new IllegalStateException("MQ down")).when(broker).convertAndSend(anyString(), anyString(), any(Object.class));
        doThrow(new IllegalStateException("serialization down")).when(json).writeValueAsString(any());
        try (MockedStatic<ChatWebSocketHandler> ws = mockStatic(ChatWebSocketHandler.class)) {
            assertDoesNotThrow(this::send); assertEquals(1, rows()); ws.verifyNoInteractions();
        }
    }

    @Test void rollbackNeverAttemptsWebSocketFallback() {
        doThrow(new IllegalStateException("MQ down")).when(broker).convertAndSend(anyString(), anyString(), any(Object.class));
        try (MockedStatic<ChatWebSocketHandler> ws = mockStatic(ChatWebSocketHandler.class)) {
            outer.executeWithoutResult(status -> { send(); status.setRollbackOnly(); });
            assertEquals(0, rows()); noEffects(); ws.verifyNoInteractions();
        }
    }

    private void finalAi() {
        AIResponseVO response = new AIResponseVO(); response.setContent("Final reply"); response.setUsage("tokens=2");
        when(ai.generateText(any())).thenReturn(response);
    }

    @Test void aiReplyHasTimestampAndPreservesFinalOutputAndIdentity() {
        finalAi(); MessageVO vo = service.sendAIMessage(7L, 8L, "Question");
        assertNotNull(vo.getCreatedAt()); assertEquals("Final reply", vo.getContent());
        assertEquals("AI助手", vo.getSenderNickname()); assertEquals("ai_reply", vo.getType());
        assertEquals(8L, vo.getSenderId()); assertEquals(7L, vo.getReceiverId());
        assertEquals(1, rows()); assertEquals("Final reply", jdbc.queryForObject("SELECT content FROM user_message", String.class));
        noEffects(); verify(aes, never()).encrypt(anyString());
    }

    @Test void aiZeroInsertCannotReturnSuccessfulReply() {
        finalAi(); doReturn(0).when(messages).insert(any(UserMessage.class));
        assertThrows(BusinessException.class, () -> service.sendAIMessage(7L, 8L, "Question")); noEffects();
    }

    @Test void aiDatabaseFailureCannotReturnSuccessfulReply() {
        finalAi(); doThrow(new IllegalStateException("database down")).when(messages).insert(any(UserMessage.class));
        assertThrows(BusinessException.class, () -> service.sendAIMessage(7L, 8L, "Question"));
        assertEquals(0, rows()); noEffects();
    }

    @Test void aiReplyParticipatesInExistingOuterRollbackWithoutNotification() {
        finalAi(); outer.executeWithoutResult(status -> { service.sendAIMessage(7L, 8L, "Question"); status.setRollbackOnly(); });
        assertEquals(0, rows()); noEffects();
    }
}
