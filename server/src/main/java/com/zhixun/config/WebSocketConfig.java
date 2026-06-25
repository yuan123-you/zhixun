package com.zhixun.config;

import com.zhixun.common.util.JwtUtil;
import com.zhixun.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServletServerHttpRequest;
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
@Slf4j
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtUtil jwtUtil;

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
                        // 握手前进行 Token 鉴权（应用层二次验证）
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            String token = servletRequest.getServletRequest().getParameter("token");
                            if (token == null || token.isEmpty()) {
                                log.warn("WebSocket 握手拒绝：缺少 token，来源 IP: {}", request.getRemoteAddress());
                                return false;
                            }
                            if (!jwtUtil.validateAccessToken(token)) {
                                log.warn("WebSocket 握手拒绝：token 无效或已过期");
                                return false;
                            }
                            // 将用户信息存入 attributes 供后续 WebSocket 会话使用
                            Long userId = jwtUtil.getUserIdFromAccessToken(token);
                            if (userId != null) {
                                attributes.put("userId", userId);
                            }
                            return true;
                        }
                        log.warn("WebSocket 握手拒绝：无法获取 ServletRequest");
                        return false;
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
