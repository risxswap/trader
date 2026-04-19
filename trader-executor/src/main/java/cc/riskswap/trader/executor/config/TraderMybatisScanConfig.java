package cc.riskswap.trader.executor.config;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.datasource.support.AutoConfiguredMapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@Configuration(proxyBeanMethods = false)
public class TraderMybatisScanConfig {

    @Bean(name = "mysqlMapperScannerConfigurer")
    public AutoConfiguredMapperScannerConfigurer mysqlMapperScannerConfigurer() {
        AutoConfiguredMapperScannerConfigurer configurer = new AutoConfiguredMapperScannerConfigurer();
        configurer.setBasePackage("cc.riskswap.trader.base.dao.mapper");
        configurer.setNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
        configurer.setAnnotationClass(MysqlMapper.class);
        configurer.setSqlSessionTemplateBeanName("mysqlSqlSessionTemplate");
        return configurer;
    }

    @Bean(name = "clickHouseMapperScannerConfigurer")
    public AutoConfiguredMapperScannerConfigurer clickHouseMapperScannerConfigurer() {
        AutoConfiguredMapperScannerConfigurer configurer = new AutoConfiguredMapperScannerConfigurer();
        configurer.setBasePackage("cc.riskswap.trader.base.dao.mapper");
        configurer.setNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
        configurer.setAnnotationClass(ClickHouseMapper.class);
        configurer.setSqlSessionTemplateBeanName("clickHouseSqlSessionTemplate");
        return configurer;
    }
}

