package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthSuccessHandler authSuccessHandler;
    private final AuthFailureHandler authFailureHandler;

    public SecurityConfig(AuthSuccessHandler authSuccessHandler, AuthFailureHandler authFailureHandler) {
        this.authSuccessHandler = authSuccessHandler;
        this.authFailureHandler = authFailureHandler;
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
                .authorizeHttpRequests(authz -> authz
                        // 公开路径
                        .requestMatchers("/", "/index.html", "/login", "/register",
                                "/forgot-password", "/verify-token",
                                "/reset-password",
                                "/css/**", "/js/**", "/images/**",
                                "/uploads/**", "/h2-console/**").permitAll()
                        // Android REST API：登录/注册公开，其余需认证
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
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
                // API 请求未认证时返回 JSON 401，而非重定向到 HTML 登录页
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
                );
        return http.build();
    }
}
