package com.zhixun.config;

import com.zhixun.common.util.JwtUtil;
import com.zhixun.websocket.ChatWebSocketHandler;
import com.zhixun.websocket.GroupChatWebSocketHandler;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 配置
 * 基于 STOMP 协议的 WebSocket 消息代理
 * 同时注册原始 WebSocket 端点用于私信和群聊功能
 */
@Slf4j
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final GroupChatWebSocketHandler groupChatWebSocketHandler;
    private final JwtUtil jwtUtil;

    // ========== WebSocketConfigurer（原始 WebSocket） ==========

    /**
     * 注册原始 WebSocket 端点（用于私信和群聊功能）
     */
    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new CookieTokenHandshakeInterceptor(jwtUtil))
                .setAllowedOriginPatterns("*");
        registry.addHandler(groupChatWebSocketHandler, "/ws/group-chat")
                .addInterceptors(new CookieTokenHandshakeInterceptor(jwtUtil))
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
                .addInterceptors(new HttpSessionHandshakeInterceptor(), new CookieTokenHandshakeInterceptor(jwtUtil))
                .withSockJS();
    }

    /**
     * 统一的 Cookie/URL Token 握手拦截器
     * 优先从 Cookie 读取 accessToken，其次从 URL 参数读取（兼容旧版本）
     * 将 userId 和 handshakeCookies 存入 WebSocketSession attributes 供后续 Handler 使用
     */
    static class CookieTokenHandshakeInterceptor implements HandshakeInterceptor {

        private final JwtUtil jwtUtil;

        CookieTokenHandshakeInterceptor(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                String token = null;

                // 优先从 Cookie 读取 accessToken（支持客户端和管理员端不同 Cookie 名称）
                Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                if (cookies != null) {
                    // 保存 Cookie 到 attributes，供后续 Handler 使用
                    attributes.put("handshakeCookies", cookies);
                    for (Cookie cookie : cookies) {
                        String name = cookie.getName();
                        if ("client_accessToken".equals(name) || "admin_accessToken".equals(name) || "accessToken".equals(name)) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }

                // Cookie 中无 token，尝试从 URL 参数读取（兼容旧版本）
                if (token == null || token.isEmpty()) {
                    token = servletRequest.getServletRequest().getParameter("token");
                }

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

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // 无需处理
        }
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
