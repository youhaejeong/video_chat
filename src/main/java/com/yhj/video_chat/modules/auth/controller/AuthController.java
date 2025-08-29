package com.yhj.video_chat.modules.auth.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	// 로그인 테스트
	@PostMapping(path = "/auth/login")
	public TokenRespose login(@RequestParam String username) throws Exception {

		// 아이디 중복확인
		userRepository.findByUsername(username).ifPresent(user -> {
			throw new RuntimeException("이미 데이터가 존재하는 유저입니다.");
		});

		// 신규 저장
		User user = User.builder().username(username).password("123").role("ROLE_USER").build();
		userRepository.save(user);

		// JWT 토큰
		String accessToken = jwtTokenProvider.createToken(username, "ROEL_USER");
		String refreshToken = jwtTokenProvider.createRefreshToken(username);
		return new TokenRespose(accessToken, refreshToken);
	}

	@PostMapping("/auth/refresh")
	public TokenRespose refresh(@RequestParam String refreshToken) throws Exception {
		// 리프레쉬 토큰 검증
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new RuntimeException("리프레시 토큰이 유효하지 않음");
		}
		// 새 액세스 토큰 발급
		String newAccessToken = jwtTokenProvider.reissueAccessToken(refreshToken);

		// 새 리프레쉬 토큰 발급(레디스 갱신
		String username = jwtTokenProvider.getUsername(refreshToken);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(username);
		return new TokenRespose(newAccessToken, newRefreshToken);

	}

	@PostMapping(path = "/auth/logout")
	public String logout(@RequestParam String username) {
		// 1. 레디스에 토큰 존재 확인 후 삭제
		String redisKey = "RT:" + username;
		Boolean hasKey = redisTemplate.hasKey(redisKey); // 레디스의 키 존재 여부 확인

		if (Boolean.TRUE.equals(hasKey)) {
			jwtTokenProvider.deleteRefreshToken(username);
		} else {
			throw new RuntimeException("누구 삭제중임?");
		}

		// 2. db 에 사용자 존재 확인 후 삭제
		// 존재하지 않는 사용자 삭제시 에러
		userRepository.findByUsername(username).map(user -> {
			userRepository.delete(user);
			return user;
		}).orElseThrow(() -> new RuntimeException("존재하지 않는 사용자임"));

		return username + "로그아웃 성공";
	}

}
