package com.yhj.video_chat.modules.auth.controller;

import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.yhj.video_chat.modules.auth.TokenRespose;
import com.yhj.video_chat.test.user.User;
import com.yhj.video_chat.test.user.UserRepository;
import com.yhj.video_chat.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 로그인
    @PostMapping("/auth/login")
    public TokenRespose login(@RequestBody Map<String, String> req) throws Exception {
        System.out.println("Login 요청 들어옴: " + req);
        String username = req.get("username");

        userRepository.findByUsername(username).ifPresent(user -> {
            throw new RuntimeException("이미 데이터가 존재하는 유저입니다.");
        });

        User user = User.builder()
                .username(username)
                .password("123")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        String accessToken = jwtTokenProvider.createToken(username, "ROLE_USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(username);

        return new TokenRespose(accessToken, refreshToken);
    }

    // 토큰 리프레시
    @PostMapping("/auth/refresh")
    public TokenRespose refresh(@RequestBody Map<String, String> req) throws Exception {
        String refreshToken = req.get("refreshToken");

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 유효하지 않음");
        }

        String newAccessToken = jwtTokenProvider.reissueAccessToken(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);

        return new TokenRespose(newAccessToken, newRefreshToken);
    }

    // 로그아웃
    @PostMapping("/auth/logout")
    public String logout(@RequestBody Map<String, String> req) {
        String username = req.get("username");

        String redisKey = "RT:" + username;
        Boolean hasKey = redisTemplate.hasKey(redisKey);

        if (Boolean.TRUE.equals(hasKey)) {
            jwtTokenProvider.deleteRefreshToken(username);
        } else {
            throw new RuntimeException("누구 삭제중임?");
        }

        userRepository.findByUsername(username).map(user -> {
            userRepository.delete(user);
            return user;
        }).orElseThrow(() -> new RuntimeException("존재하지 않는 사용자임"));

        return username + " 로그아웃 성공";
    }
}
