package cc.riskswap.trader.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class TaskConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core thread pool size
        executor.setCorePoolSize(10);
        // Maximum thread pool size
        executor.setMaxPoolSize(20);
        // Queue capacity
        executor.setQueueCapacity(200);
        // Keep alive time
        executor.setKeepAliveSeconds(60);
        // Thread name prefix
        executor.setThreadNamePrefix("taskExecutor-");
        // Rejection policy: Caller runs
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean("correlationExecutor")
    public Executor correlationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core thread pool size
        executor.setCorePoolSize(5);
        // Maximum thread pool size
        executor.setMaxPoolSize(10);
        // Queue capacity
        executor.setQueueCapacity(500);
        // Keep alive time
        executor.setKeepAliveSeconds(60);
        // Thread name prefix
        executor.setThreadNamePrefix("correlation-");
        // Rejection policy: Caller runs
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
