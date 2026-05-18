package com.example.demo;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("2640967068@qq.com");
        message.setTo(toEmail);
        message.setSubject("【美食打卡】密码重置验证码");
        message.setText("您正在重置密码，验证码为：\n\n" + code + "\n\n验证码有效期为 5 分钟，请勿泄露给他人。\n如非本人操作，请忽略此邮件。");
        mailSender.send(message);
    }
}
