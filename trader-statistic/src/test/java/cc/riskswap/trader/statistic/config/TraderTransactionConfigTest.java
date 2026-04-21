package cc.riskswap.trader.statistic.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TraderTransactionConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TraderTransactionConfig.class)
            .withBean("mysqlDataSource", DataSource.class, () -> mock(DataSource.class))
            .withBean("clickHouseDataSource", DataSource.class, () -> mock(DataSource.class));

    @Test
    void should_register_mysql_transaction_manager() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("transactionManager");
            assertThat(context).hasBean("mysqlTransactionManager");
            assertThat(context).hasBean("clickHouseTransactionManager");
        });
    }
}
