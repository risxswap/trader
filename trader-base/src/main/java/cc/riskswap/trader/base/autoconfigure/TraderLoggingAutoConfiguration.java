package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.event.TraderStreamPublisher;
import cc.riskswap.trader.base.logging.TraderTaskLogAspect;
import cc.riskswap.trader.base.logging.TraderThreadContextTaskDecorator;
import cc.riskswap.trader.base.config.TraderNodeProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskDecorator;

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnProperty(prefix = "trader.task-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraderLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraderTaskLogAspect traderTaskLogAspect(
            ObjectProvider<TraderStreamPublisher> traderStreamPublisherProvider,
            TraderNodeProperties nodeProperties) {
        String appName = nodeProperties.getName();
        String taskType = nodeProperties.getType();
        return new TraderTaskLogAspect(traderStreamPublisherProvider.getIfAvailable(), appName, taskType);
    }

    @Bean
    @ConditionalOnMissingBean(name = "traderThreadContextTaskDecorator")
    public TaskDecorator traderThreadContextTaskDecorator() {
        return new TraderThreadContextTaskDecorator();
    }
}
