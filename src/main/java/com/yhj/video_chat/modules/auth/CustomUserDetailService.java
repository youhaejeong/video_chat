package com.yhj.video_chat.modules.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.yhj.video_chat.test.user.User;
import com.yhj.video_chat.test.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService{
	
	private final UserRepository repository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user=repository.findByUsername(username).orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."+ username));
		return new CustomUserDetails(user);
	}

}
