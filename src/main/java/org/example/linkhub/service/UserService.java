package org.example.linkhub.service;

import org.example.linkhub.entity.User;
import org.example.linkhub.exception.BusinessException;
import org.example.linkhub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    public static final String DEMO_USER_NAME = "alice";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Phase 2 会替换为从 SecurityContext 解析 JWT 登录用户
    public User getCurrentUser() {
        return userRepository.findByUserName(DEMO_USER_NAME)
                .orElseGet(() -> {
                    log.warn("演示用户不存在，自动创建（Phase 2 接入 JWT 登录后可移除）");
                    return userRepository.save(User.builder()
                            .userName(DEMO_USER_NAME)
                            .password(passwordEncoder.encode("123456"))
                            .email("alice@example.com")
                            .build());
                });
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }
}
