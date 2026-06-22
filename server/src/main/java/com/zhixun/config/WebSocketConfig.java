package com.zhixun.config;

import com.zhixun.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 配置
 * 基于 STOMP 协议的 WebSocket 消息代理
 * 同时注册原始 WebSocket 端点用于私信功能
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    // ========== WebSocketConfigurer（原始 WebSocket） ==========

    /**
     * 注册原始 WebSocket 端点（用于私信功能）
     */
    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*");
    }

    // ========== WebSocketMessageBrokerConfigurer（STOMP） ==========

    /**
     * 注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                                   org.springframework.http.server.ServerHttpResponse response,
                                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) {
                        // 握手前可在此处进行鉴权
                        return true;
                    }
                })
                .withSockJS();
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端发送消息的前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 服务端广播消息的前缀
        registry.enableSimpleBroker("/topic", "/queue");
        // 点对点消息前缀
        registry.setUserDestinationPrefix("/user");
    }
}
