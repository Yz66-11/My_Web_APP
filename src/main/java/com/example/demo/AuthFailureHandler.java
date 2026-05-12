package com.example.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserRepository userRepository;

    public AuthFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String errorMessage;

        if (username != null && !userRepository.existsByUsername(username)) {
            errorMessage = "该账号未注册，请先注册后再登录！";
        } else {
            errorMessage = "用户名或密码错误，请重新输入！";
        }

        setDefaultFailureUrl("/login?error=" + java.net.URLEncoder.encode(
                errorMessage, java.nio.charset.StandardCharsets.UTF_8));

        super.onAuthenticationFailure(request, response, exception);
    }
}
