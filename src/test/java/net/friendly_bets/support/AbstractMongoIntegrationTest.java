package net.friendly_bets.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Базовый класс для интеграционных тестов с локальным mongod (по умолчанию).
 * <p>
 * URI: {@code mongodb://localhost:27017/friendly_bets_integration} — отдельная БД, не пересекается с dev {@code /test}.
 * Перед каждым тестом все коллекции в этой БД удаляются.
 * <p>
 * Подключение в Compass: {@code mongodb://localhost:27017} → база {@code friendly_bets_integration}.
 * <p>
 * Чтобы использовать Testcontainers (Docker), запускайте Maven с {@code -Dtest.mongo.mode=testcontainers}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractMongoIntegrationTest {

    private static final String LOCAL_TEST_URI = "mongodb://localhost:27017/friendly_bets_integration";

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void registerMongoUri(DynamicPropertyRegistry registry) {
        if ("testcontainers".equalsIgnoreCase(System.getProperty("test.mongo.mode"))) {
            registry.add("spring.data.mongodb.uri", TestcontainersMongo::getUri);
        } else {
            String overrideUri = System.getProperty("test.mongo.uri");
            registry.add("spring.data.mongodb.uri",
                    () -> overrideUri != null && !overrideUri.isBlank() ? overrideUri : LOCAL_TEST_URI);
        }
    }

    @BeforeEach
    void cleanIntegrationDatabase() {
        mongoTemplate.getDb().drop();
    }

    /**
     * Ленивый запуск MongoDB в Docker только при {@code -Dtest.mongo.mode=testcontainers}.
     */
    static final class TestcontainersMongo {
        private static final org.testcontainers.containers.MongoDBContainer CONTAINER =
                new org.testcontainers.containers.MongoDBContainer("mongo:7.0");

        static String getUri() {
            if (!CONTAINER.isRunning()) {
                CONTAINER.start();
            }
            return CONTAINER.getConnectionString();
        }

        private TestcontainersMongo() {
        }
    }
}
