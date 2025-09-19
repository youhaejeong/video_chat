package com.yhj.video_chat.modules.videoChatRoom;


import com.yhj.video_chat.modules.auth.User;
import com.yhj.video_chat.modules.videoChatRoom.dto.RoomRequest;
import com.yhj.video_chat.modules.videoChatRoom.dto.RoomResponse;
import com.yhj.video_chat.test.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/room")
public class VideoChatRoomController {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 방 생성 (Broadcaster)
    @PostMapping("/create")
    public RoomResponse createRoom(@RequestBody RoomRequest request) throws Exception {
        String username = request.getUsername();
        String roomId = request.getRoomId();

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));

        user.setRole("ROLE_BROADCASTER");
        userRepository.save(user);

        String roomKey = "room:" + roomId + ":participants";
        redisTemplate.opsForList().rightPush(roomKey, username);

        List<String> participants = redisTemplate.opsForList().range(roomKey, 0, -1);

        return RoomResponse.builder().roomId(roomId).owner(username).participants(participants).message(username + "님이 방을 생성했습니다.").build();
    }

    // 방 입장 (Viewer)
    @PostMapping("/join")
    public RoomResponse joinRoom(@RequestBody RoomRequest request) throws Exception {
        String username = request.getUsername();
        String roomId = request.getRoomId();

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));

        user.setRole("ROLE_VIEWER");
        userRepository.save(user);

        String roomKey = "room:" + roomId + ":participants";
        redisTemplate.opsForList().rightPush(roomKey, username);

        List<String> participants = redisTemplate.opsForList().range(roomKey, 0, -1);

        String owner = participants.get(0); // 첫 번째 참여자를 방장으로 가정

        return RoomResponse.builder().roomId(roomId).owner(owner).participants(participants).message(username + "님이 방에 입장했습니다.").build();
    }

    // 방 나가기
    @PostMapping("/leave")
    public RoomResponse leaveRoom(@RequestBody RoomRequest request) throws Exception {
        String username = request.getUsername();
        String roomId = request.getRoomId();

        String roomKey = "room:" + roomId + ":participants";

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));

        // 참가자 목록에서 제거
        redisTemplate.opsForList().remove(roomKey, 1, username);



        // Broadcaster가 나간 경우
        if ("ROLE_BROADCASTER".equals(user.getRole())) {
            List<String> participants = redisTemplate.opsForList().range(roomKey, 0, -1);

            // 방 삭제
            redisTemplate.delete(roomKey);
            // 역할 초기화
            user.setRole("ROLE_NONE");
            userRepository.save(user);


            return RoomResponse.builder()
                    .roomId(roomId)
                    .owner(null)
                    .participants(participants)
                    .message("방장이 나갔으므로 방이 종료되었습니다.")
                    .build();
        }

        // 역할 초기화 (DB에서 삭제 안함)
        user.setRole("ROLE_NONE");
        userRepository.save(user);

        // 일반 Viewer가 나간 경우
        List<String> participants = redisTemplate.opsForList().range(roomKey, 0, -1);
        String owner = participants != null && !participants.isEmpty() ? participants.get(0) : null;

        return RoomResponse.builder().roomId(roomId).owner(owner).participants(participants).message(username + "님이 방을 나갔습니다.").build();
    }


    // 방 목록 조회 (추가)
    @GetMapping("/list")
    public List<String> getRoomList() {
        // Redis에서 방 키 목록 조회 (room:*:participants)
        return redisTemplate.keys("room:*:participants").stream().map(key -> key.split(":")[1]) // roomId만 추출
                .toList();
    }

}
