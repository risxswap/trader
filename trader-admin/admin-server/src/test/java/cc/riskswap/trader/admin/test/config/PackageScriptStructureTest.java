package cc.riskswap.trader.admin.test.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class PackageScriptStructureTest {

    @Test
    void should_build_admin_server_from_repository_root_reactor() throws Exception {
        String script = Files.readString(Path.of("..", "package.sh"));

        Assertions.assertTrue(script.contains("-f \"${ROOT_DIR}/../pom.xml\""));
        Assertions.assertTrue(script.contains("-pl trader-admin/admin-server -am"));
        Assertions.assertFalse(script.contains("-pl admin-server -am"));
    }
}
