package com.jnta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApplicationContextTest {

    @Test
    @DisplayName("given PostgreSQL and Elasticsearch are available, when the application starts, then the context loads")
    void shouldLoadApplicationContext() {
    }
}
