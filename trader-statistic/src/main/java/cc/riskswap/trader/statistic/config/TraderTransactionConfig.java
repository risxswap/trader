package cc.riskswap.trader.statistic.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
public class TraderTransactionConfig {

    @Bean(name = {"transactionManager", "mysqlTransactionManager"})
    @ConditionalOnBean(name = "mysqlDataSource")
    @ConditionalOnMissingBean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(Objects.requireNonNull(dataSource));
    }

    @Bean(name = "clickHouseTransactionManager")
    @ConditionalOnBean(name = "clickHouseDataSource")
    @ConditionalOnMissingBean(name = "clickHouseTransactionManager")
    public PlatformTransactionManager clickHouseTransactionManager(@Qualifier("clickHouseDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(Objects.requireNonNull(dataSource));
    }
}
