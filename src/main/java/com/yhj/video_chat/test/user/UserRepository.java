package com.yhj.video_chat.test.user;

import java.util.Optional;

import com.yhj.video_chat.modules.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	void deleteByUsername(String username);
}
