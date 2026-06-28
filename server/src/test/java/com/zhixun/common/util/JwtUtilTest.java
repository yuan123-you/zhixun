package com.zhixun.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试（使用 Mockito + 反射注入配置值）
 */
@DisplayName("JwtUtil 测试")
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    /** 测试用 Base64 密钥 (32字节) */
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes());

    @BeforeEach
    void setUp() {
        // 通过反射注入 @Value 字段
        ReflectionTestUtils.setField(jwtUtil, "accessSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "refreshSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessExpiration", 604800L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800L);
    }

    @Nested
    @DisplayName("Token 生成")
    class TokenGeneration {

        @Test
        @DisplayName("应生成有效的访问令牌")
        void shouldGenerateAccessToken() {
            String token = jwtUtil.generateAccessToken(1L, "testuser");
            assertNotNull(token, "令牌不应为 null");
            assertFalse(token.isEmpty(), "令牌不应为空");
        }

        @Test
        @DisplayName("应生成有效的刷新令牌")
        void shouldGenerateRefreshToken() {
            String token = jwtUtil.generateRefreshToken(1L, "testuser");
            assertNotNull(token, "令牌不应为 null");
            assertFalse(token.isEmpty(), "令牌不应为空");
        }

        @Test
        @DisplayName("每次生成的令牌应不同（包含 nonce）")
        void shouldGenerateUniqueTokens() {
            String token1 = jwtUtil.generateAccessToken(1L, "testuser");
            String token2 = jwtUtil.generateAccessToken(1L, "testuser");
            assertNotEquals(token1, token2, "相同用户多次生成的令牌应不同");
        }

        @Test
        @DisplayName("应生成带角色的令牌")
        void shouldGenerateTokenWithRole() {
            String token = jwtUtil.generateAccessToken(1L, "testuser", "admin");
            Claims claims = jwtUtil.parseAccessToken(token);
            assertNotNull(claims, "Claims 不应为 null");
            assertEquals("admin", claims.get(JwtUtil.CLAIM_ROLE), "角色应为 admin");
        }
    }

    @Nested
    @DisplayName("Token 验证")
    class TokenValidation {

        @Test
        @DisplayName("有效访问令牌应验证通过")
        void shouldValidateAccessToken() {
            String token = jwtUtil.generateAccessToken(1L, "testuser");
            assertTrue(jwtUtil.validateAccessToken(token), "有效访问令牌应验证通过");
        }

        @Test
        @DisplayName("有效刷新令牌应验证通过")
        void shouldValidateRefreshToken() {
            String token = jwtUtil.generateRefreshToken(1L, "testuser");
            assertTrue(jwtUtil.validateRefreshToken(token), "有效刷新令牌应验证通过");
        }

        @Test
        @DisplayName("无效令牌应验证失败")
        void shouldRejectInvalidToken() {
            assertFalse(jwtUtil.validateAccessToken("invalid.token.here"),
                    "无效令牌应验证失败");
        }

        @Test
        @DisplayName("空令牌应验证失败")
        void shouldRejectEmptyToken() {
            // JwtUtil 内部未处理空/空白字符串，jjwt 会抛出 IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> {
                jwtUtil.validateAccessToken("");
            }, "空令牌应抛出 IllegalArgumentException");
        }

        @Test
        @DisplayName("null 令牌应验证失败")
        void shouldRejectNullToken() {
            assertThrows(IllegalArgumentException.class, () -> {
                jwtUtil.validateAccessToken(null);
            }, "null 令牌应抛出 IllegalArgumentException");
        }

        @Test
        @DisplayName("访问令牌不能作为刷新令牌验证")
        void shouldRejectAccessTokenAsRefreshToken() {
            String accessToken = jwtUtil.generateAccessToken(1L, "testuser");
            assertFalse(jwtUtil.validateRefreshToken(accessToken),
                    "访问令牌不应作为刷新令牌验证通过");
        }

        @Test
        @DisplayName("刷新令牌不能作为访问令牌验证")
        void shouldRejectRefreshTokenAsAccessToken() {
            String refreshToken = jwtUtil.generateRefreshToken(1L, "testuser");
            assertFalse(jwtUtil.validateAccessToken(refreshToken),
                    "刷新令牌不应作为访问令牌验证通过");
        }
    }

    @Nested
    @DisplayName("Token 解析")
    class TokenParsing {

        @Test
        @DisplayName("应从访问令牌中获取用户ID")
        void shouldGetUserIdFromAccessToken() {
            String token = jwtUtil.generateAccessToken(42L, "testuser");
            Long userId = jwtUtil.getUserIdFromAccessToken(token);
            assertEquals(42L, userId, "应正确获取用户ID");
        }

        @Test
        @DisplayName("应从访问令牌中获取用户名")
        void shouldGetUsernameFromAccessToken() {
            String token = jwtUtil.generateAccessToken(1L, "testuser");
            String username = jwtUtil.getUsernameFromAccessToken(token);
            assertEquals("testuser", username, "应正确获取用户名");
        }

        @Test
        @DisplayName("应从访问令牌中获取角色")
        void shouldGetRoleFromAccessToken() {
            String token = jwtUtil.generateAccessToken(1L, "testuser", "admin");
            String role = jwtUtil.getRoleFromAccessToken(token);
            assertEquals("admin", role, "应正确获取角色");
        }

        @Test
        @DisplayName("无效令牌解析应返回 null")
        void shouldReturnNullForInvalidToken() {
            assertNull(jwtUtil.getUserIdFromAccessToken("invalid"), "无效令牌应返回 null");
            assertNull(jwtUtil.getUsernameFromAccessToken("invalid"), "无效令牌应返回 null");
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("大用户ID应正常处理")
        void shouldHandleLargeUserId() {
            Long largeId = Long.MAX_VALUE;
            String token = jwtUtil.generateAccessToken(largeId, "testuser");
            assertTrue(jwtUtil.validateAccessToken(token), "大用户ID令牌应有效");
            assertEquals(largeId, jwtUtil.getUserIdFromAccessToken(token));
        }

        @Test
        @DisplayName("特殊字符用户名应正常处理")
        void shouldHandleSpecialUsernames() {
            String username = "test@user.name";
            String token = jwtUtil.generateAccessToken(1L, username);
            assertTrue(jwtUtil.validateAccessToken(token), "特殊字符用户名令牌应有效");
            assertEquals(username, jwtUtil.getUsernameFromAccessToken(token));
        }

        @Test
        @DisplayName("中文用户名应正常处理")
        void shouldHandleChineseUsernames() {
            String username = "测试用户";
            String token = jwtUtil.generateAccessToken(1L, username);
            assertTrue(jwtUtil.validateAccessToken(token), "中文用户名令牌应有效");
            assertEquals(username, jwtUtil.getUsernameFromAccessToken(token));
        }
    }
}