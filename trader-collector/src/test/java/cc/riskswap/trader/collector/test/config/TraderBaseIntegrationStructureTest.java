package cc.riskswap.trader.collector.test.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TraderBaseIntegrationStructureTest {

    @Test
    void pomUsesTraderBaseJarAndJava21() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<artifactId>trader-base</artifactId>"));
        assertTrue(pom.contains("<version>1.0.0-SNAPSHOT</version>"));
        assertTrue(pom.contains("<java.version>21</java.version>"));
    }

    @Test
    void applicationYmlUsesTraderBasePropertyKeys() throws IOException {
        String applicationYml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(applicationYml.contains("trader:"));
        assertTrue(applicationYml.contains("mysql:"));
        assertTrue(applicationYml.contains("clickhouse:"));
        assertTrue(applicationYml.contains("redis:"));
        assertTrue(applicationYml.contains("monitor:"));
        assertTrue(applicationYml.contains("node:"));
        assertTrue(applicationYml.contains("id:"));
        assertFalse(applicationYml.contains("spring:\n  datasource:"));
        assertFalse(applicationYml.contains("spring:\r\n  datasource:"));
    }

    @Test
    void removesLocalEnvironmentPostProcessorRegistration() {
        assertFalse(Files.exists(Path.of("src/main/resources/META-INF/spring.factories")));
    }
}
