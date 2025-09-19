package com.yhj.video_chat.modules.videoChatRoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomResponse {
    private String roomId;          // 방 ID
    private String owner;           // 방장(username)
    private List<String> participants; // 참여자 리스트
    private String message;         // 상황 메시지 (방 생성/입장/퇴장 등)
}
