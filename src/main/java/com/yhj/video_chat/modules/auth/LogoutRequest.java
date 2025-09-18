package com.yhj.video_chat.modules.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequest {
    private String username;
    private String roomId;
}