package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthSuccessHandler authSuccessHandler;
    private final AuthFailureHandler authFailureHandler;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(AuthSuccessHandler authSuccessHandler,
                          AuthFailureHandler authFailureHandler,
                          JwtAuthFilter jwtAuthFilter) {
        this.authSuccessHandler = authSuccessHandler;
        this.authFailureHandler = authFailureHandler;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 暴露 AuthenticationManager，供 ApiController 的 /api/auth/login 使用 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // API 请求不依赖 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(authz -> authz
                        // 公开路径
                        .requestMatchers("/", "/index.html", "/login", "/register",
                                "/forgot-password", "/verify-token",
                                "/reset-password",
                                "/terms", "/privacy",
                                "/css/**", "/js/**", "/images/**", "/audio/**",
                                "/uploads/**", "/h2-console/**").permitAll()
                        // Android REST API：登录/注册/刷新Token/忘记密码/重置密码公开
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/verify-reset-code",
                                "/api/auth/reset-password",
                                "/api/auth/send-register-code").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // 管理员专属
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Web 页面需登录
                        .requestMatchers("/merchants", "/merchant/**", "/profile",
                                "/food-gallery", "/food-gallery/**", "/checkin", "/shop/**", "/my-shops",
                                "/posts", "/post/**", "/update-profile", "/change-password",
                                "/upload-avatar"
                                ).authenticated()
                        .anyRequest().authenticated()
                )
                // API 请求未认证时返回 JSON 401
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.getWriter().write("{\"error\":\"未登录\"}");
                                },
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authSuccessHandler)
                        .failureHandler(authFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // 注册 JWT 认证过滤器，在用户名密码认证之前执行
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
