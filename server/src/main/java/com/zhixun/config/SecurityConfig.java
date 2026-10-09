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
     * 放行的公开静态/工具接口
     * 注意：认证接口已使用显式 HTTP 方法匹配，防止框架版本差异导致 403
     */
    private static final String[] PUBLIC_URLS = {
            // Swagger / Knife4j
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**",
            // WebSocket
            "/ws/**",
            // 静态资源
            "/static/**",
            // 本地文件上传目录（MinIO 不可用时的降级方案）
            "/uploads/**",
            "/favicon.ico",
            // 健康检查
            "/actuator/**"
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
                // 启用 CORS（依赖 CorsConfig 中定义的 CorsFilter Bean）
                .cors(c -> {})
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
                        // 认证相关 POST 接口放行（显式声明 HTTP 方法，防止框架歧义）
                        .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/send-code").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/v1/auth/forgot-password").permitAll()
                        // 认证相关 GET 接口放行
                        .requestMatchers(HttpMethod.GET, "/v1/auth/graph-captcha").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/auth/oauth/url").permitAll()
                        // OAuth 公开接口
                        .requestMatchers(HttpMethod.POST, "/v1/auth/oauth/login").permitAll()
                        // 公开静态资源/Swagger/WebSocket 放行
                        .requestMatchers("/doc.html", "/swagger-ui/**", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**", "/ws/**", "/static/**",
                                "/uploads/**", "/v1/files/view/**",
                                "/favicon.ico", "/actuator/**").permitAll()
                        // 作品列表和详情（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/articles", "/v1/articles/**").permitAll()
                        // 分类列表和分类树（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/categories", "/v1/categories/tree").permitAll()
                        // 标签列表、热门标签、标签云、搜索标签（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/tags", "/v1/tags/hot", "/v1/tags/cloud", "/v1/tags/search").permitAll()
                        // 信息流（推荐、最新、热门公开，关注需认证）
                        .requestMatchers(HttpMethod.GET, "/v1/feed/recommend", "/v1/feed/latest", "/v1/feed/hot").permitAll()
                        // 排行榜（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/rank/**").permitAll()
                        // 搜索（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/search", "/v1/search/suggest", "/v1/search/hot").permitAll()
                        // 轮播图（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/banners").permitAll()
                        // 公告（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/announcements").permitAll()
                        // 推荐用户（公开，需在 /v1/users/** ADMIN规则之前）
                        .requestMatchers(HttpMethod.GET, "/v1/users/recommend").permitAll()
                        // 匿名 IP 属地查询（公开）
                        .requestMatchers(HttpMethod.GET, "/v1/users/ip-location").permitAll()
                        // 用户关注/在线状态（需认证，需在 /v1/users/** ADMIN规则之前）
                        .requestMatchers(HttpMethod.POST, "/v1/users/{id}/follow").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/users/{id}/following", "/v1/users/{id}/followers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/users/{id}/online-status").authenticated()
                        // 用户信息查看（公开，需在 /v1/users/** ADMIN规则之前）
                        .requestMatchers(HttpMethod.GET, "/v1/users/{id}").permitAll()
                        // 当前登录用户信息（需认证，需在 /v1/users/** ADMIN规则之前）
                        .requestMatchers("/v1/users/me").authenticated()
                        // 用户个人数据接口（需认证，/v1/user/** 路径）
                        .requestMatchers("/v1/user/**").authenticated()
                        // 管理端接口 - 仅 ADMIN 角色可访问
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        // 用户管理 - 仅 ADMIN 角色可访问
                        .requestMatchers("/v1/users/**").hasRole("ADMIN")
                        // 操作日志 - 仅 ADMIN 角色可访问
                        .requestMatchers("/v1/operation-logs/**").hasRole("ADMIN")
                        // 登录日志 - 仅 ADMIN 角色可访问
                        .requestMatchers("/v1/login-logs/**").hasRole("ADMIN")
                        // 敏感词管理 - 仅 ADMIN 角色可访问
                        .requestMatchers("/v1/sensitive-words/**").hasRole("ADMIN")
                        // 浏览历史 - 仅允许用户访问自己的数据
                        .requestMatchers("/v1/view-history/**").authenticated()
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
                .addPathPatterns("/v1/articles/**", "/v1/comments/**", "/v1/messages/**",
                        "/v1/collects/**", "/v1/users/**", "/v1/user/**");
    }
}
