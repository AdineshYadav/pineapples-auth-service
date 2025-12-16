package com.bakery.autoBackery.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.sql.Date;
import java.time.Instant;
import java.util.Map;

@Service
public class JwtService {

    private final Key key;
    private final long accessExpMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-exp-min}") long accessExpMinutes){
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpMinutes = accessExpMinutes;

    }

    public String generateAccessToken(String subject, Map<String, Object> claims){
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpMinutes * 60);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(claims)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

    }

    public Jws<Claims> parse(String token) throws JwtException{
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public long getAccessExpSeconds() {
        return accessExpMinutes * 60;
    }

}


