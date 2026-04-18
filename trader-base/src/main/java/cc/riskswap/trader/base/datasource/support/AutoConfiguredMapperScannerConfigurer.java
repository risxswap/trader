package cc.riskswap.trader.base.datasource.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.util.StringUtils;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class AutoConfiguredMapperScannerConfigurer extends MapperScannerConfigurer implements BeanFactoryAware {

    private String configuredBasePackage;
    private ConfigurableListableBeanFactory beanFactory;
    private boolean autoResolvedBasePackage;

    @Override
    public void setBasePackage(String basePackage) {
        this.configuredBasePackage = basePackage;
        this.autoResolvedBasePackage = false;
        super.setBasePackage(basePackage);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
            this.beanFactory = configurableListableBeanFactory;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!resolveBasePackage()) {
            return;
        }
        super.afterPropertiesSet();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        if (!resolveBasePackage()) {
            return;
        }
        if (autoResolvedBasePackage) {
            super.setProcessPropertyPlaceHolders(false);
        }
        super.postProcessBeanDefinitionRegistry(registry);
    }

    private boolean resolveBasePackage() {
        if (StringUtils.hasText(configuredBasePackage)) {
            return true;
        }
        if (beanFactory == null || !AutoConfigurationPackages.has(beanFactory)) {
            return false;
        }
        String basePackage = String.join(",", AutoConfigurationPackages.get(beanFactory));
        if (!StringUtils.hasText(basePackage)) {
            return false;
        }
        this.configuredBasePackage = basePackage;
        this.autoResolvedBasePackage = true;
        super.setBasePackage(basePackage);
        return true;
    }
}
