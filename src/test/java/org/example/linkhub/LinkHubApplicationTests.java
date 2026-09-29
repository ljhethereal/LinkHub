package org.example.linkhub;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("需要本地 PostgreSQL + Redis，启动 docker-compose 后再运行")
class LinkHubApplicationTests {

    @Test
    void contextLoads() {
    }

}
