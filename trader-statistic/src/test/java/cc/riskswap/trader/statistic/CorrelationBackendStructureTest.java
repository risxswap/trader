package cc.riskswap.trader.statistic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CorrelationBackendStructureTest {

    @Test
    void shouldProvideCorrelationBackendDaoTypes() {
        Assertions.assertNotNull(load("cc.riskswap.trader.base.dao.CorrelationDao"));
        Assertions.assertNotNull(load("cc.riskswap.trader.base.dao.mapper.CorrelationMapper"));
        Assertions.assertNotNull(load("cc.riskswap.trader.base.dao.FundDao"));
        Assertions.assertNotNull(load("cc.riskswap.trader.base.dao.FundNavDao"));
        Assertions.assertNotNull(load("cc.riskswap.trader.statistic.service.CorrelationService"));
        Assertions.assertNotNull(load("cc.riskswap.trader.statistic.task.CorrelationTask"));
        Assertions.assertNotNull(load("cc.riskswap.trader.statistic.config.TaskConfig"));
    }

    private Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
