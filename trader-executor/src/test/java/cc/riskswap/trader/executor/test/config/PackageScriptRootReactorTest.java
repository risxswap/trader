package cc.riskswap.trader.executor.test.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class PackageScriptRootReactorTest {

    @Test
    void shouldBuildFromRootReactor() throws Exception {
        String script = Files.readString(Path.of("package.sh"));

        Assertions.assertTrue(script.contains("-f \"$ROOT_DIR/../pom.xml\""));
        Assertions.assertTrue(script.contains("-pl trader-executor -am -DskipTests package"));
        Assertions.assertFalse(script.contains("\"$MVNW\" -DskipTests package"));
    }
}
