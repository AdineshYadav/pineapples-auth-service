package com.bakery.autoBackery.service;


import com.bakery.autoBackery.dto.AuthResponse;
import com.bakery.autoBackery.dto.LoginRequest;
import com.bakery.autoBackery.dto.RefreshRequest;
import com.bakery.autoBackery.dto.RegisterRequest;
import com.bakery.autoBackery.model.RefreshToken;
import com.bakery.autoBackery.model.Role;
import com.bakery.autoBackery.model.User;
import com.bakery.autoBackery.repository.RefreshTokenRepository;
import com.bakery.autoBackery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    @Value("${app.jwt.refresh-token-exp-days}")
    private long refreshExpDays;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role role = req.getRole() == null ? Role.CUSTOMER : req.getRole();
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .enabled(true)
                .build();
        userRepo.save(user);
        return issueTokens(user);

    }


    public AuthResponse login(LoginRequest req){
        User user = userRepo.findByUsername(req.getUsernameOrEmail())
                .or(()->userRepo.findByEmail(req.getUsernameOrEmail()))
                .orElseThrow(()->new IllegalArgumentException("Invalid credentials"));

        if(!passwordEncoder.matches(req.getPassword() , user.getPasswordHash())){
            throw new IllegalArgumentException("Invalid credentials");
        }
        if(!user.isEnabled()){
            throw new IllegalArgumentException("User disabled");
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest req){
        RefreshToken rt = refreshRepo.findByToken(req.getRefreshToken())
                .orElseThrow(()->new IllegalArgumentException("Invalid refresh token"));

        if(rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())){
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        User user = rt.getUser();
        String access = jwt.generateAccessToken(user.getUsername(), Map.of(
                "role" , user.getRole().name(),
                "uid" , user.getId()
        ));
        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(rt.getToken())
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInSeconds(jwt.getAccessExpSeconds())
                .build();
    }

    public void revokeRefreshToken(String token){
        refreshRepo.findByToken(token).ifPresent(rt->{
            rt.setRevoked(true);
            refreshRepo.save(rt);
        });
    }

    private AuthResponse issueTokens(User user){
        String access = jwt.generateAccessToken(user.getUsername(),Map.of(
                "role" , user.getRole().name(),
                "uid" , user.getId()
        ));
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(refreshExpDays*24*3600))
                .revoked(false)
                .build();
        refreshRepo.save(rt);

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(rt.getToken())
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInSeconds(jwt.getAccessExpSeconds())
                .build();
    }


}
