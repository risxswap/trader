package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.dao.TaskLogDao;
import cc.riskswap.trader.base.logging.TaskLogStore;
import cc.riskswap.trader.base.logging.TraderTaskLogAspect;
import cc.riskswap.trader.base.logging.TraderThreadContextTaskDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.Nullable;

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnProperty(prefix = "trader.task-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraderLoggingAutoConfiguration {

    @Bean
    @ConditionalOnBean(TaskLogDao.class)
    @ConditionalOnMissingBean
    public TaskLogStore taskLogStore(TaskLogDao taskLogDao) {
        return new TaskLogStore(taskLogDao);
    }

    @Bean
    @ConditionalOnMissingBean
    public TraderTaskLogAspect traderTaskLogAspect(@Nullable TaskLogStore taskLogStore) {
        return new TraderTaskLogAspect(taskLogStore);
    }

    @Bean
    @ConditionalOnMissingBean(name = "traderThreadContextTaskDecorator")
    public TaskDecorator traderThreadContextTaskDecorator() {
        return new TraderThreadContextTaskDecorator();
    }
}
