package com.zhixun.config;

import com.zhixun.security.CsrfFilter;
import com.zhixun.security.DataPermissionInterceptor;
import com.zhixun.security.JwtAccessDeniedHandler;
import com.zhixun.security.JwtAuthenticationFilter;
import com.zhixun.security.JwtUnauthorizedHandler;
import com.zhixun.security.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Security 配置
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtUnauthorizedHandler jwtUnauthorizedHandler;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CsrfFilter csrfFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final DataPermissionInterceptor dataPermissionInterceptor;

    /**
     * 放行的公开接口
     * 注意：server.servlet.context-path=/api，所以路径需包含 /api 前缀
     */
    private static final String[] PUBLIC_URLS = {
            // 认证相关
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/send-code",
            "/api/v1/auth/graph-captcha",
            "/api/v1/auth/forgot-password",
            // Swagger / Knife4j
            "/api/doc.html",
            "/api/swagger-ui/**",
            "/api/swagger-resources/**",
            "/api/v3/api-docs/**",
            "/api/webjars/**",
            // WebSocket
            "/api/ws/**",
            // 静态资源
            "/api/static/**",
            "/api/favicon.ico",
            // 健康检查
            "/api/actuator/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 Spring Security 内置 CSRF（使用自定义双重提交 Cookie 模式替代）
                .csrf(AbstractHttpConfigurer::disable)
                // 关闭表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 关闭 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 无状态会话
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtUnauthorizedHandler)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        // 预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 公开接口放行
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // 文章列表和详情（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/articles", "/api/v1/articles/**").permitAll()
                        // 分类列表和分类树（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/tree").permitAll()
                        // 标签列表、热门标签、标签云、搜索标签（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/tags/hot", "/api/v1/tags/cloud", "/api/v1/tags/search").permitAll()
                        // 信息流（推荐、最新、热门公开，关注需认证）
                        .requestMatchers(HttpMethod.GET, "/api/v1/feed/recommend", "/api/v1/feed/latest", "/api/v1/feed/hot").permitAll()
                        // 排行榜（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/rank/**").permitAll()
                        // 搜索（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/search", "/api/v1/search/suggest", "/api/v1/search/hot").permitAll()
                        // 轮播图（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners").permitAll()
                        // 公告（公开）
                        .requestMatchers(HttpMethod.GET, "/api/v1/announcements").permitAll()
                        // 推荐用户（公开，需在 /api/v1/users/** ADMIN规则之前）
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/recommend").permitAll()
                        // 用户关注/在线状态（需认证，需在 /api/v1/users/** ADMIN规则之前）
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/{id}/follow").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}/following", "/api/v1/users/{id}/followers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}/online-status").authenticated()
                        // 用户信息查看（公开，需在 /api/v1/users/** ADMIN规则之前）
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").permitAll()
                        // 管理端接口 - 仅 ADMIN 角色可访问
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 用户管理 - 仅 ADMIN 角色可访问
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        // 操作日志 - 仅 ADMIN 角色可访问
                        .requestMatchers("/api/v1/operation-logs/**").hasRole("ADMIN")
                        // 登录日志 - 仅 ADMIN 角色可访问
                        .requestMatchers("/api/v1/login-logs/**").hasRole("ADMIN")
                        // 敏感词管理 - 仅 ADMIN 角色可访问
                        .requestMatchers("/api/v1/sensitive-words/**").hasRole("ADMIN")
                        // 浏览历史 - 仅允许用户访问自己的数据
                        .requestMatchers("/api/v1/view-history/**").authenticated()
                        // 其他接口需要认证
                        .anyRequest().authenticated());

        // 添加 JWT 过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 添加 CSRF 双重提交 Cookie 过滤器（在 JWT 过滤器之后）
        http.addFilterAfter(csrfFilter, JwtAuthenticationFilter.class);

        // 添加安全响应头过滤器（最先执行）
        http.addFilterBefore(securityHeadersFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器 - BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 注册数据权限拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dataPermissionInterceptor)
                .addPathPatterns("/api/v1/articles/**", "/api/v1/comments/**", "/api/v1/messages/**",
                        "/api/v1/collects/**", "/api/v1/users/**", "/api/v1/user/**");
    }
}
