package cc.riskswap.trader.base.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigLoader implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String DEFAULT_CONFIG_PATH = "/opt/trader/config.properties";
    private static final String CONFIG_PATH_PROPERTY = "config.path";
    private static final String NODE_CONFIG_PATH_PROPERTY = "node.config.path";
    private static final String PROPERTY_SOURCE_NAME = "trader-common";
    private static final String NODE_PROPERTY_SOURCE_NAME = "trader-node";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String commonConfigPath = resolvePath(environment, CONFIG_PATH_PROPERTY, DEFAULT_CONFIG_PATH);
        loadProperties(environment, commonConfigPath, PROPERTY_SOURCE_NAME);

        String nodeConfigPath = environment.getProperty(NODE_CONFIG_PATH_PROPERTY);
        if (StringUtils.hasText(nodeConfigPath)) {
            loadProperties(environment, nodeConfigPath, NODE_PROPERTY_SOURCE_NAME);
        }
    }

    private String resolvePath(ConfigurableEnvironment environment, String propertyName, String defaultPath) {
        String configuredPath = environment.getProperty(propertyName);
        if (StringUtils.hasText(configuredPath)) {
            return configuredPath;
        }
        emitInfo("{} is not set, use default path: {}", propertyName, defaultPath);
        return defaultPath;
    }

    private void loadProperties(ConfigurableEnvironment environment, String configPath, String sourceName) {
        emitInfo("Load external config from {}", configPath);
        File external = new File(configPath);
        if (!external.exists() || !external.isFile()) {
            emitInfo("Config file not found: {}", configPath);
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(external);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            props.load(reader);
            if (environment.getPropertySources().contains(sourceName)) {
                environment.getPropertySources().remove(sourceName);
            }
            environment.getPropertySources().addFirst(new PropertiesPropertySource(sourceName, props));
            emitInfo("Loaded {} properties from {}", props.size(), configPath);
        } catch (IOException exception) {
            log.warn("Failed to load config from {}", configPath, exception);
        }
    }

    private void emitInfo(String template, Object... args) {
        log.info(template, args);
        System.out.println("[trader-base] " + format(template, args));
    }

    private String format(String template, Object... args) {
        String message = template;
        if (args == null) {
            return message;
        }
        for (Object arg : args) {
            message = message.replaceFirst("\\{}", java.util.regex.Matcher.quoteReplacement(String.valueOf(arg)));
        }
        return message;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
