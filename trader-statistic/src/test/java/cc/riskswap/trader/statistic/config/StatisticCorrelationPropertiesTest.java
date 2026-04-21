package cc.riskswap.trader.statistic.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StatisticCorrelationPropertiesTest {

    @Test
    void should_use_default_nav_query_code_batch_size() {
        StatisticCorrelationProperties properties = new StatisticCorrelationProperties();

        Assertions.assertEquals(200, properties.getNavQueryCodeBatchSize());
    }

    @Test
    void should_use_default_clickhouse_batch_sizes() {
        StatisticCorrelationProperties properties = new StatisticCorrelationProperties();

        Assertions.assertEquals(200, properties.getSaveBatchSize());
        Assertions.assertEquals(200, properties.getCleanupDeleteBatchSize());
    }
}
