package cc.riskswap.trader.admin.system;

import cc.riskswap.trader.admin.config.DatabaseScriptSupport;
import cc.riskswap.trader.admin.service.UserService;
import cc.riskswap.trader.admin.service.UpgradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemInitializer implements ApplicationRunner {
    private final DatabaseInitializer databaseInitializer;
    private final UserService userService;
    private final UpgradeService upgradeService;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (DatabaseScriptSupport.isSpringBootTestEnvironment(environment.getProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper"))) {
            return;
        }
        databaseInitializer.initializeOnStartup();
        userService.initAdmin();
        try {
            upgradeService.runOnStartup();
        } catch (Throwable ignored) {
        }
    }
}
