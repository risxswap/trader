package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.datasource.clickhouse.TraderClickHouseProperties;
import cc.riskswap.trader.base.datasource.mysql.TraderMysqlProperties;
import cc.riskswap.trader.base.datasource.support.AutoConfiguredMapperScannerConfigurer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SqlSessionFactory.class, MybatisSqlSessionFactoryBean.class})
@EnableConfigurationProperties({TraderMysqlProperties.class, TraderClickHouseProperties.class})
public class TraderDataSourceAutoConfiguration {

    @Bean(name = "mysqlDataSource")
    @ConditionalOnProperty(prefix = "trader.mysql", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "trader.mysql", name = "url")
    @ConditionalOnMissingBean(name = "mysqlDataSource")
    public DataSource mysqlDataSource(TraderMysqlProperties traderMysqlProperties) {
        return createMysqlDataSource(traderMysqlProperties);
    }

    @Bean(name = "clickHouseDataSource")
    @ConditionalOnProperty(prefix = "trader.clickhouse", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "trader.clickhouse", name = "url")
    @ConditionalOnMissingBean(name = "clickHouseDataSource")
    public DataSource clickHouseDataSource(TraderClickHouseProperties traderClickHouseProperties) {
        return createClickHouseDataSource(traderClickHouseProperties);
    }

    @Bean(name = "mysqlMybatisPlusInterceptor")
    @ConditionalOnMissingBean(name = "mysqlMybatisPlusInterceptor")
    @ConditionalOnBean(name = "mysqlDataSource")
    public MybatisPlusInterceptor mysqlMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean(name = "clickHouseMybatisPlusInterceptor")
    @ConditionalOnMissingBean(name = "clickHouseMybatisPlusInterceptor")
    @ConditionalOnBean(name = "clickHouseDataSource")
    public MybatisPlusInterceptor clickHouseMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean(name = "mysqlSqlSessionFactory")
    @ConditionalOnBean(name = "mysqlDataSource")
    @ConditionalOnMissingBean(name = "mysqlSqlSessionFactory")
    public SqlSessionFactory mysqlSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource,
                                                    @Qualifier("mysqlMybatisPlusInterceptor") Interceptor interceptor) throws Exception {
        return createSqlSessionFactory(dataSource, interceptor);
    }

    @Bean(name = "clickHouseSqlSessionFactory")
    @ConditionalOnBean(name = "clickHouseDataSource")
    @ConditionalOnMissingBean(name = "clickHouseSqlSessionFactory")
    public SqlSessionFactory clickHouseSqlSessionFactory(@Qualifier("clickHouseDataSource") DataSource dataSource,
                                                         @Qualifier("clickHouseMybatisPlusInterceptor") Interceptor interceptor) throws Exception {
        return createSqlSessionFactory(dataSource, interceptor);
    }

    @Bean(name = "mysqlSqlSessionTemplate")
    @ConditionalOnBean(name = "mysqlSqlSessionFactory")
    @ConditionalOnMissingBean(name = "mysqlSqlSessionTemplate")
    public SqlSessionTemplate mysqlSqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "clickHouseSqlSessionTemplate")
    @ConditionalOnBean(name = "clickHouseSqlSessionFactory")
    @ConditionalOnMissingBean(name = "clickHouseSqlSessionTemplate")
    public SqlSessionTemplate clickHouseSqlSessionTemplate(@Qualifier("clickHouseSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "mysqlMapperScannerConfigurer")
    @ConditionalOnBean(name = "mysqlSqlSessionTemplate")
    @ConditionalOnMissingBean(name = "mysqlMapperScannerConfigurer")
    public AutoConfiguredMapperScannerConfigurer mysqlMapperScannerConfigurer() {
        AutoConfiguredMapperScannerConfigurer configurer = new AutoConfiguredMapperScannerConfigurer();
        configurer.setAnnotationClass(MysqlMapper.class);
        configurer.setSqlSessionTemplateBeanName("mysqlSqlSessionTemplate");
        configurer.setProcessPropertyPlaceHolders(true);
        return configurer;
    }

    @Bean(name = "clickHouseMapperScannerConfigurer")
    @ConditionalOnBean(name = "clickHouseSqlSessionTemplate")
    @ConditionalOnMissingBean(name = "clickHouseMapperScannerConfigurer")
    public AutoConfiguredMapperScannerConfigurer clickHouseMapperScannerConfigurer() {
        AutoConfiguredMapperScannerConfigurer configurer = new AutoConfiguredMapperScannerConfigurer();
        configurer.setAnnotationClass(ClickHouseMapper.class);
        configurer.setSqlSessionTemplateBeanName("clickHouseSqlSessionTemplate");
        configurer.setProcessPropertyPlaceHolders(true);
        return configurer;
    }

    private SqlSessionFactory createSqlSessionFactory(DataSource dataSource, Interceptor interceptor) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPlugins(interceptor);
        return factoryBean.getObject();
    }

    private DataSource createMysqlDataSource(TraderMysqlProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        configureDataSource(dataSource, properties.getUrl(), properties.getUsername(), properties.getPassword(), properties.getDriverClassName());
        return dataSource;
    }

    private DataSource createClickHouseDataSource(TraderClickHouseProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        configureDataSource(dataSource, properties.getUrl(), properties.getUsername(), properties.getPassword(), properties.getDriverClassName());
        return dataSource;
    }

    private void configureDataSource(HikariDataSource dataSource, String url, String username, String password, String driverClassName) {
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (StringUtils.hasText(driverClassName)) {
            dataSource.setDriverClassName(driverClassName);
        }
    }
}
