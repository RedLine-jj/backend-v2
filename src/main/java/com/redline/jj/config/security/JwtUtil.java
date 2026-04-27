package com.redline.jj.config.security;

import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String userId) {
        return buildToken(userId, accessTokenExpiration);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, refreshTokenExpiration);
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isExpired(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private String buildToken(String userId, long expiration) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiration))
            .signWith(key)
            .compact();
    }
}
