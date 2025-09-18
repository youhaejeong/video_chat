package com.yhj.video_chat.modules.auth.controller;

import java.util.Map;

import com.yhj.video_chat.modules.auth.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    public TokenRespose login(@RequestBody LoginRequest request) throws Exception {
        System.out.println("Login 요청 들어옴: " + request);

        String username = request.getUsername();
        String roomId = request.getRoomId();

        // 이미 존재하는 유저 체크
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new RuntimeException("이미 데이터가 존재하는 유저입니다.");
        });

        // Redis에서 참가자 수 확인
        String roomKey = "room:" + roomId + ":participants";
        Long participantCount = redisTemplate.opsForList().size(roomKey);

        String role = (participantCount == null || participantCount == 0) ? "ROLE_BROADCASTER" : "ROLE_VIEWER";

        // DB 저장
        User user = User.builder()
                .username(username)
                .password("123")
                .role(role)
                .build();
        userRepository.save(user);

        // Redis에 참가자 추가
        redisTemplate.opsForList().rightPush(roomKey, username);

        // JWT 생성
        String accessToken = jwtTokenProvider.createToken(username, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(username);

        return new TokenRespose(accessToken, refreshToken,role);
    }


    // 토큰 리프레시
    @PostMapping("/auth/refresh")
    public TokenRespose refresh(@RequestBody RefreshRequest request) throws Exception {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 유효하지 않음");
        }


        String newAccessToken = jwtTokenProvider.reissueAccessToken(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);

        // username으로 DB에서 role 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));
        String role = user.getRole();

        return new TokenRespose(newAccessToken, newRefreshToken,role);
    }


    // 로그아웃
    @PostMapping("/auth/logout")
    public String logout(@RequestBody LogoutRequest request) {
        String username = request.getUsername();
        String roomId = request.getRoomId();

        // RefreshToken 삭제
        String redisKey = "RT:" + username;
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(hasKey)) {
            jwtTokenProvider.deleteRefreshToken(username);
        }

        // Redis에서 참가자 제거
        String roomKey = "room:" + roomId + ":participants";
        redisTemplate.opsForList().remove(roomKey, 1, username);

        // DB에서 사용자 제거
        userRepository.findByUsername(username).map(user -> {
            userRepository.delete(user);
            return user;
        }).orElseThrow(() -> new RuntimeException("존재하지 않는 사용자임"));

        return username + " 로그아웃 성공";
    }

}
