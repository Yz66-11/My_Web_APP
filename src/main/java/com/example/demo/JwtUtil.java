package com.example.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT 工具类 — 生成 / 校验 Access Token 和 Refresh Token
 * <p>
 * Access Token 有效期 15 分钟，用于认证 API 请求
 * Refresh Token 有效期 7 天，用于无感刷新 Access Token
 * </p>
 */
public class JwtUtil {

    // 至少 256-bit 的密钥（Base64 编码）
    private static final String SECRET_BASE64 = "dGhpcyBpcyBhIHNlY3JldCBrZXkgZm9yIGp3dCB0b2tlbiBnZW5lcmF0aW9uIDI1NiBiaXRz";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));

    // Access Token 有效期：15 分钟
    private static final long ACCESS_EXPIRATION_MS = 15 * 60 * 1000L;
    // Refresh Token 有效期：7 天
    private static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    /** 生成 Access Token */
    public static String generateAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    /** 生成 Refresh Token */
    public static String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    /** 从 Token 中提取用户名（subject） */
    public static String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** 验证 Token 是否有效且未过期 */
    public static boolean isTokenValid(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 判断 Token 是否已过期 */
    public static boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
