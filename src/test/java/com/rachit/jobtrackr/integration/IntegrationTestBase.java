package com.rachit.jobtrackr.integration;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared base class for all Testcontainers integration tests.
 *
 * Design decisions:
 * - Static containers: reused across ALL test classes for speed (JVM-scoped lifecycle).
 *   Testcontainers' Ryuk reaper handles cleanup after the JVM exits.
 * - @DynamicPropertySource: injects container URLs into Spring context at startup.
 * - Mock GoogleAiGeminiChatModel: avoids real Gemini API calls during tests.
 *   Returns a neutral JSON response that the EmailIngestionService can parse.
 * - @ActiveProfiles("integration"): uses application-integration.yml overrides.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
@Import(IntegrationTestBase.TestConfig.class)
public abstract class IntegrationTestBase {

    // ── Static containers — shared across all test classes ──────────────────
    // Using static so they start once per JVM, not once per test class.

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("jobtrackr_test")
                    .withUsername("jobtrackr")
                    .withPassword("jobtrackr");

    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
                    .withKraft();

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        // Redis — disable for integration tests (use no-op cache)
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    /**
     * Provides a mock Gemini chat model so tests don't call the real API.
     * The mock returns a neutral "OTHER" classification for email ingestion tests.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public GoogleAiGeminiChatModel mockGeminiChatModel() {
            GoogleAiGeminiChatModel mock = mock(GoogleAiGeminiChatModel.class);
            when(mock.chat(anyString()))
                    .thenReturn("{\"classification\": \"OTHER\", \"company\": \"\", \"role\": \"\"}");
            return mock;
        }
    }
}
