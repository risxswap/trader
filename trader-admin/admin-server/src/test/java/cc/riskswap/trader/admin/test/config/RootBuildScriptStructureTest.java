package cc.riskswap.trader.admin.test.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class RootBuildScriptStructureTest {

    @Test
    void should_support_package_all_command_in_root_build_script() throws Exception {
        String script = Files.readString(Path.of("..", "..", "build.sh"));

        Assertions.assertTrue(script.contains("package|package-all|full-install"));
        Assertions.assertTrue(script.contains("./build.sh package-all"));
        Assertions.assertTrue(script.contains("if [[ \"$command\" == \"package-all\" ]]"));
        Assertions.assertTrue(script.contains("for module in \"${SUPPORTED_MODULES[@]}\""));
    }

    @Test
    void should_document_package_all_command_in_readme() throws Exception {
        String readme = Files.readString(Path.of("..", "..", "README.md"));

        Assertions.assertTrue(readme.contains("./build.sh package-all"));
    }
}
