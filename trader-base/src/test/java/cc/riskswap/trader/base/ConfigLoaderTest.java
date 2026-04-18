package cc.riskswap.trader.base;

import cc.riskswap.trader.base.config.ConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadExternalPropertiesIntoEnvironment() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "trader.name=demo\ncustom.flag=true\n");

        MockEnvironment environment = new MockEnvironment();
        TestPropertyValues.of("config.path=" + configFile).applyTo(environment);

        new ConfigLoader().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("trader.name")).isEqualTo("demo");
        assertThat(environment.getProperty("custom.flag")).isEqualTo("true");
    }

    @Test
    void shouldLoadChinesePropertiesWithoutGarbledText() throws IOException {
        Path configFile = tempDir.resolve("config-zh.properties");
        Files.writeString(configFile, "trader.node.name=测试节点\ntrader.node.type=做市\n");

        MockEnvironment environment = new MockEnvironment();
        TestPropertyValues.of("config.path=" + configFile).applyTo(environment);

        new ConfigLoader().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("trader.node.name")).isEqualTo("测试节点");
        assertThat(environment.getProperty("trader.node.type")).isEqualTo("做市");
    }

    @Test
    void shouldIgnoreMissingExternalFile() {
        MockEnvironment environment = new MockEnvironment();
        TestPropertyValues.of("config.path=" + tempDir.resolve("missing.properties")).applyTo(environment);

        new ConfigLoader().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("trader.name")).isNull();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldPrintLoadedPropertyCount(CapturedOutput output) throws IOException {
        Path configFile = tempDir.resolve("config-count.properties");
        Files.writeString(configFile, "trader.name=demo\ncustom.flag=true\n");

        MockEnvironment environment = new MockEnvironment();
        TestPropertyValues.of("config.path=" + configFile).applyTo(environment);

        new ConfigLoader().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(output.getOut()).contains("Loaded 2 properties from");
        assertThat(output.getOut()).contains("[trader-base] Loaded 2 properties from");
    }

    @Test
    void shouldOverrideCommonPropertiesWithNodeProperties() throws IOException {
        Path commonConfigFile = tempDir.resolve("common.properties");
        Files.writeString(commonConfigFile, "trader.node.name=common-node\ntrader.feature.enabled=false\n");
        Path nodeConfigFile = tempDir.resolve("node.properties");
        Files.writeString(nodeConfigFile, "trader.node.name=node-a\ntrader.node.type=maker\n");

        MockEnvironment environment = new MockEnvironment();
        TestPropertyValues.of(
                "config.path=" + commonConfigFile,
                "node.config.path=" + nodeConfigFile
        ).applyTo(environment);

        new ConfigLoader().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("trader.node.name")).isEqualTo("node-a");
        assertThat(environment.getProperty("trader.node.type")).isEqualTo("maker");
        assertThat(environment.getProperty("trader.feature.enabled")).isEqualTo("false");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldSkipNodeConfigLoadingWhenNodeConfigPathNotConfigured(CapturedOutput output) throws IOException {
        Path commonConfigFile = tempDir.resolve("common-only.properties");
        Files.writeString(commonConfigFile, "trader.name=demo\n");

        MockEnvironment environment = new MockEnvironment();
        TestPropertyValues.of("config.path=" + commonConfigFile).applyTo(environment);

        new ConfigLoader().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(output.getOut()).doesNotContain("node.config.path is not set");
        assertThat(output.getOut()).doesNotContain("/opt/trader/node-config.properties");
    }
}
