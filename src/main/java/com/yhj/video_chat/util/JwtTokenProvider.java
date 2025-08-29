package com.yhj.video_chat.util;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.yhj.video_chat.VideoChatApplication;
import com.yhj.video_chat.modules.auth.CustomUserDetailService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final VideoChatApplication videoChatApplication;

	private final RedisTemplate<String, String> redisTemplate;

	private final SecretKey secretKey = Keys
			.hmacShaKeyFor("your-256-bit-secret-key-your-256-bit-secret-key".getBytes());
//	private final long validityInMillisecodes = 1000L * 60 * 60; // 1시간
	private final long validityInMillisecodes = 1000L * 15; // 15초

	private final long refreshTokenValidity = 1000L * 60 * 60 * 24 * 7; // 7일

	private final CustomUserDetailService customUserDetailService;

	// JWT 생성
	public String createToken(String username, String role) {
		Claims claims = Jwts.claims().setSubject(username);
		claims.put("role", role);

		Date now = new Date();
		Date validity = new Date(now.getTime() + validityInMillisecodes);

		return Jwts.builder().setClaims(claims).setIssuedAt(now).setExpiration(validity)
				.signWith(secretKey, SignatureAlgorithm.HS256).compact();
	}

	// token 에서 username 추출
	public String getUsername(String token) {
		Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
		return claims.getSubject();
	}

	// jwt 에서 인증 정보 추출
	public Authentication getAuthentication(String token) {
		String username = getUsername(token);
		UserDetails userDetails = customUserDetailService.loadUserByUsername(username);
		return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
	}

	// token 유효성 확인
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
			return true;

		} catch (Exception e) {
			return false;
		}
	}

	// 리프레쉬 토큰 생성
	public String createRefreshToken(String username) {

		Date now = new Date();
		Date validity = new Date(now.getTime() + refreshTokenValidity);
		// 리프레쉬 토큰 생성
		String refreshToken = Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(validity)
				.signWith(secretKey, SignatureAlgorithm.HS256).compact();

		// 레디스에 저장
		redisTemplate.opsForValue().set("RT:" + username, // key
				refreshToken, // value
				refreshTokenValidity, // timeout
				TimeUnit.MILLISECONDS // 단위
		);

		return refreshToken;
	}

	// 리프레쉬 토큰 검증
	public boolean validateRefreshToken(String refreshToken) {
		try {
			// JWT 자체 유효성 확인(서명+만료)
			if (!validateToken(refreshToken))
				return false;
			// Redis 에서 저장된 토큰 확인
			String username = getUsername(refreshToken);
			String savedToken = redisTemplate.opsForValue().get("RT:" + username);
			return refreshToken.equals(savedToken);
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}

	}

	// 리프레쉬 token 으로 access token 재발급
	public String reissueAccessToken(String refreshToken) {
		if (!validateRefreshToken(refreshToken)) {
			throw new RuntimeException("리프레쉬 토큰 재발급 필요");
		}
		String username = getUsername(refreshToken);
		UserDetails userDetails = customUserDetailService.loadUserByUsername(username);
		return createToken(userDetails.getUsername(), userDetails.getAuthorities().iterator().next().getAuthority());
	}

	// 로그아웃시 리프레쉬 토큰 삭제
	public void deleteRefreshToken(String username) {
		redisTemplate.delete("RT:" + username);
	}
}
