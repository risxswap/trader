package cc.riskswap.trader.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class PackagingStructureTest {

    @Test
    void shouldProvideCollectorStylePackagingFiles() {
        Path projectRoot = Path.of("").toAbsolutePath();

        Assertions.assertTrue(Files.exists(projectRoot.resolve("package.sh")));
        Assertions.assertTrue(Files.exists(projectRoot.resolve("docker-compose.yml")));
        Assertions.assertTrue(Files.exists(projectRoot.resolve("bin/run.sh")));
    }
}
