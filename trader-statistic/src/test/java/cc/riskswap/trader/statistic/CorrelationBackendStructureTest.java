package cc.riskswap.trader.statistic;

import cc.riskswap.trader.base.dao.mapper.CorrelationMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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

    @Test
    void shouldWaitForClickHouseCorrelationDeletesToFinish() throws Exception {
        Assertions.assertTrue(updateSql("deleteByIds", java.util.List.class).contains("mutations_sync = 1"));
        Assertions.assertTrue(updateSql("deleteByPrimaryId", Long.class).contains("mutations_sync = 1"));
    }

    private Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private String updateSql(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = CorrelationMapper.class.getMethod(methodName, parameterTypes);
        Update update = method.getAnnotation(Update.class);
        Assertions.assertNotNull(update);
        return String.join(" ", update.value());
    }
}
