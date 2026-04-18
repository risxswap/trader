package cc.riskswap.trader.collector.test.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PackageScriptStructureTest {

    private static final String CURRENT_PACKAGE_NAME = "trader-collector";
    private static final String LEGACY_PACKAGE_NAME = "trader-" + "fund";

    @Test
    void packageScriptUsesProjectMavenWrapperAndSafetyGuards() throws Exception {
        String script = Files.readString(Path.of("package.sh"));

        assertTrue(script.contains("set -euo pipefail"));
        assertTrue(script.contains("ROOT_DIR="));
        assertTrue(script.contains("ARTIFACT_ID="));
        assertTrue(script.contains("./mvnw -DskipTests package"));
        assertTrue(script.contains("PACKAGE_NAME=\"trader-collector\""));
        assertTrue(script.contains("PKG_DIR=\"${DIST_DIR}/${PACKAGE_NAME}\""));
        assertTrue(script.contains("TAR_PATH=\"${DIST_DIR}/${PACKAGE_NAME}.tar.gz\""));
        assertTrue(script.contains("if [ ! -f \"$MVNW\" ]; then"));
        assertTrue(script.contains("jar tf \"$JAR_PATH\" >/dev/null 2>&1"));
        assertTrue(script.contains("mkdir -p \"${PKG_DIR}/config\""));
        assertTrue(script.contains("cp -f \"$APPLICATION_YML\" \"${PKG_DIR}/config/application.yml\""));
        assertTrue(script.contains("cp -f \"$LOGBACK_XML\" \"${PKG_DIR}/config/logback-spring.xml\""));
        assertTrue(script.contains("cp -R \"$ROOT_DIR/bin\" \"${PKG_DIR}/bin\""));
        assertFalse(script.contains("README.md"));
    }

    @Test
    void deploymentAssetsUseTraderCollectorName() throws Exception {
        assertUsesTraderCollector(Path.of("package.sh"));
        assertUsesTraderCollector(Path.of("pom.xml"));
        assertUsesTraderCollector(Path.of("docker-compose.yml"));
    }

    @Test
    void dockerComposePersistsNodeIdInConfigPropertiesAndUsesNodeConfigPath() throws Exception {
        String dockerCompose = Files.readString(Path.of("docker-compose.yml"));

        assertTrue(dockerCompose.contains("./bin:/app/bin"));
        assertTrue(dockerCompose.contains("./config:/app/config"));
        assertFalse(dockerCompose.contains("./data:/app/data"));
        assertTrue(dockerCompose.contains("sh /app/bin/run.sh"));
        String runScript = Files.readString(Path.of("bin/run.sh"));
        assertTrue(runScript.contains("CONFIG_FILE=/app/config/config.properties"));
        assertTrue(runScript.contains("NODE_KEY=trader.node.id"));
        assertTrue(runScript.contains("grep -E \"^${NODE_KEY}=\" \"$CONFIG_FILE\""));
        assertTrue(runScript.contains("if [ -n \"$EXISTING_LINE\" ]"));
        assertTrue(runScript.contains("if [ -s \"$CONFIG_FILE\" ]"));
        assertTrue(runScript.contains("printf '\\n%s=%s\\n' \"$NODE_KEY\" \"$TRADER_NODE_ID\" >> \"$CONFIG_FILE\""));
        assertTrue(runScript.contains("printf '%s=%s\\n' \"$NODE_KEY\" \"$TRADER_NODE_ID\" >> \"$CONFIG_FILE\""));
        assertTrue(runScript.contains("--node.config.path=/app/config/config.properties"));
        assertTrue(runScript.contains("--spring.config.location=file:/app/config/application.yml"));
        assertTrue(runScript.contains("--logging.config=file:/app/config/logback-spring.xml"));
        assertTrue(runScript.contains("exec java -jar trader-collector.jar"));
    }

    private void assertUsesTraderCollector(Path path) throws Exception {
        String content = Files.readString(path);

        assertTrue(content.contains(CURRENT_PACKAGE_NAME));
        assertFalse(content.contains(LEGACY_PACKAGE_NAME));
    }
}
