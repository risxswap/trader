package cc.riskswap.trader.collector.test.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import cc.riskswap.trader.collector.repository.dao.mapper.CalendarMapper;
import cc.riskswap.trader.collector.repository.dao.mapper.FundAdjMapper;
import cc.riskswap.trader.collector.repository.dao.mapper.FundMapper;
import cc.riskswap.trader.collector.repository.dao.mapper.FundMarketMapper;
import cc.riskswap.trader.collector.repository.dao.mapper.FundNavMapper;
import cc.riskswap.trader.collector.repository.dao.mapper.ImportLogMapper;

class MybatisConfigStructureTest {

    @Test
    void reliesOnTraderBaseMapperAnnotations() throws Exception {
        Class<?> mysqlMapper = Class.forName("cc.riskswap.trader.base.datasource.annotation.MysqlMapper");
        Class<?> clickHouseMapper = Class.forName("cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper");

        assertNotNull(FundMapper.class.getAnnotation(mysqlMapper.asSubclass(java.lang.annotation.Annotation.class)));
        assertNotNull(CalendarMapper.class.getAnnotation(mysqlMapper.asSubclass(java.lang.annotation.Annotation.class)));
        assertNotNull(ImportLogMapper.class.getAnnotation(mysqlMapper.asSubclass(java.lang.annotation.Annotation.class)));
        assertNotNull(FundAdjMapper.class.getAnnotation(clickHouseMapper.asSubclass(java.lang.annotation.Annotation.class)));
        assertNotNull(FundMarketMapper.class.getAnnotation(clickHouseMapper.asSubclass(java.lang.annotation.Annotation.class)));
        assertNotNull(FundNavMapper.class.getAnnotation(clickHouseMapper.asSubclass(java.lang.annotation.Annotation.class)));
    }

    @Test
    void removesLocalDualDataSourceConfigurationClasses() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.config.MybatisConfig"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.config.MysqlMapper"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("cc.riskswap.trader.collector.config.ClickHouseMapper"));
    }
}
