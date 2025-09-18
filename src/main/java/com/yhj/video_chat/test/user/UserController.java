package com.yhj.video_chat.test.user;

import java.util.List;

import com.yhj.video_chat.modules.auth.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
	
	private final UserRepository userRepository;
	
	@GetMapping(path = "/userList")
	public List<User> getUser(){
		return userRepository.findAll();
	}
	
}
