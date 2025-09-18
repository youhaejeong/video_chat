package com.yhj.video_chat.modules.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class TokenRespose {
	private String accessToken;
	private String refreshToken;
    private String role;

}
