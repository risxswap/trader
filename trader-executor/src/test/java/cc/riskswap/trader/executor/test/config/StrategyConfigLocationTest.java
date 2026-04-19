package cc.riskswap.trader.executor.test.config;

import cc.riskswap.trader.executor.strategy.config.BaseStrategyConfig;
import cc.riskswap.trader.executor.strategy.config.RelativeStrengthStrategyConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StrategyConfigLocationTest {

    @Test
    void shouldDefineStrategyConfigsInExecutorPackage() {
        Assertions.assertEquals(
                "cc.riskswap.trader.executor.strategy.config",
                BaseStrategyConfig.class.getPackageName()
        );
        Assertions.assertEquals(
                BaseStrategyConfig.class,
                RelativeStrengthStrategyConfig.class.getSuperclass()
        );
    }
}

