package org.example.linkhub.bootstrap;

import org.example.linkhub.entity.User;
import org.example.linkhub.repository.UserRepository;
import org.example.linkhub.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DemoDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final UserService userService;

    public DemoDataInitializer(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUserName(UserService.DEMO_USER_NAME).isPresent()) {
            log.info("演示用户已存在，跳过初始化");
            return;
        }
        User user = User.builder()
                .userName(UserService.DEMO_USER_NAME)
                .password(userService.passwordEncoder().encode("123456"))
                .email("alice@example.com")
                .build();
        userRepository.save(user);
        log.info("已初始化演示用户: id={}, username={}", user.getId(), user.getUserName());
    }
}
