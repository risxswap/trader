package cc.riskswap.trader.admin.test.system;

import cc.riskswap.trader.admin.service.UpgradeService;
import cc.riskswap.trader.admin.service.UserService;
import cc.riskswap.trader.admin.system.DatabaseInitializer;
import cc.riskswap.trader.admin.system.SystemInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

public class SystemInitializerTest {

    @Test
    void shouldSkipStartupTasksInSpringBootTestEnvironment() {
        DatabaseInitializer databaseInitializer = Mockito.mock(DatabaseInitializer.class);
        UserService userService = Mockito.mock(UserService.class);
        UpgradeService upgradeService = Mockito.mock(UpgradeService.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper", "true");

        SystemInitializer systemInitializer = new SystemInitializer(databaseInitializer, userService, upgradeService, environment);

        systemInitializer.run(null);

        Mockito.verifyNoInteractions(databaseInitializer, userService, upgradeService);
    }
}
