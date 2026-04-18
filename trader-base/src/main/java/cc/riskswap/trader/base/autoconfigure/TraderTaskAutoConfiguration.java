package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.event.TraderStreamPublisher;
import cc.riskswap.trader.base.task.TraderTask;
import cc.riskswap.trader.base.task.TraderTaskMetadataSyncService;
import cc.riskswap.trader.base.task.TraderTaskPoller;
import cc.riskswap.trader.base.task.TraderTaskPollingJob;
import cc.riskswap.trader.base.task.TraderTaskProperties;
import cc.riskswap.trader.base.task.TraderTaskRefreshPublisher;
import cc.riskswap.trader.base.task.TraderTaskRefreshSubscriber;
import cc.riskswap.trader.base.task.TraderTaskRegistry;
import cc.riskswap.trader.base.task.TraderTaskSchedulerService;
import cc.riskswap.trader.base.task.TraderTaskLock;
import cc.riskswap.trader.base.task.TraderTaskExecutor;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnClass(SchedulerFactoryBean.class)
@ConditionalOnProperty(prefix = "trader.task", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({TraderTaskProperties.class, cc.riskswap.trader.base.config.TraderNodeProperties.class})
public class TraderTaskAutoConfiguration {

    @Bean
    public TraderTaskRegistry traderTaskRegistry(ApplicationContext applicationContext) {
        List<TraderTask> tasks = applicationContext.getBeansOfType(TraderTask.class).values().stream().toList();
        return new TraderTaskRegistry(tasks);
    }

    @Bean
    public SchedulerFactoryBean traderTaskSchedulerFactoryBean(ApplicationContext applicationContext) {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setApplicationContext(applicationContext);
        factoryBean.setOverwriteExistingJobs(true);
        factoryBean.setAutoStartup(true);
        return factoryBean;
    }

    @Bean
    public TraderTaskSchedulerService traderTaskSchedulerService(
            cc.riskswap.trader.base.config.TraderNodeProperties nodeProperties,
            @Qualifier("traderTaskSchedulerFactoryBean") Scheduler scheduler
    ) {
        String taskType = cc.riskswap.trader.base.task.TraderTaskType.fromNodeType(nodeProperties.getType()).name();
        return new TraderTaskSchedulerService(taskType, scheduler);
    }

    @Bean
    public TraderTaskRefreshSubscriber traderTaskRefreshSubscriber(
            cc.riskswap.trader.base.config.TraderNodeProperties nodeProperties,
            StringRedisTemplate stringRedisTemplate,
            TraderTaskSchedulerService traderTaskSchedulerService
    ) {
        String taskType = cc.riskswap.trader.base.task.TraderTaskType.fromNodeType(nodeProperties.getType()).name();
        return new TraderTaskRefreshSubscriber(taskType, stringRedisTemplate, traderTaskSchedulerService);
    }

    @Bean
    public TraderTaskPoller traderTaskPoller(
            cc.riskswap.trader.base.config.TraderNodeProperties nodeProperties,
            StringRedisTemplate stringRedisTemplate,
            TraderTaskSchedulerService traderTaskSchedulerService
    ) {
        String taskType = cc.riskswap.trader.base.task.TraderTaskType.fromNodeType(nodeProperties.getType()).name();
        return new TraderTaskPoller(taskType, stringRedisTemplate, traderTaskSchedulerService);
    }

    @Bean
    public TraderTaskPollingJob traderTaskPollingJob(TraderTaskPoller traderTaskPoller) {
        return new TraderTaskPollingJob(traderTaskPoller);
    }

    @Bean
    public TraderTaskMetadataSyncService traderTaskMetadataSyncService() {
        return new TraderTaskMetadataSyncService();
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    public cc.riskswap.trader.base.task.TraderTaskDefinitionPublisher traderTaskDefinitionPublisher(
            StringRedisTemplate stringRedisTemplate,
            cc.riskswap.trader.base.config.TraderNodeProperties traderNodeProperties
    ) {
        return new cc.riskswap.trader.base.task.TraderTaskDefinitionPublisher(stringRedisTemplate, traderNodeProperties.getId(), traderNodeProperties.getType());
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    public ApplicationRunner traderTaskDefinitionPublishRunner(
            TraderTaskRegistry registry,
            cc.riskswap.trader.base.task.TraderTaskDefinitionPublisher publisher
    ) {
        return args -> publisher.publishAll(registry);
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    public TraderStreamPublisher traderStreamPublisher(StringRedisTemplate stringRedisTemplate) {
        return new TraderStreamPublisher(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    public TraderTaskLock traderTaskLock(StringRedisTemplate stringRedisTemplate, TraderTaskProperties properties) {
        return new TraderTaskLock(stringRedisTemplate, properties.getLockExpireSeconds());
    }

    @Bean
    public TraderTaskExecutor traderTaskExecutor(TraderTaskRegistry registry, StringRedisTemplate stringRedisTemplate, TraderTaskLock lock, TraderStreamPublisher streamPublisher) {
        return new TraderTaskExecutor(registry, stringRedisTemplate, lock, streamPublisher);
    }
}
