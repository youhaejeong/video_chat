package com.yhj.video_chat.modules.videoChatRoom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {
    private String roomId;
    private String username;
    private String roomName;
}
