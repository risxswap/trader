package cc.riskswap.trader.admin.config;

import cc.riskswap.trader.base.task.TraderTaskRefreshPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class TraderTaskCompatibilityConfig {

    @Bean
    @ConditionalOnMissingBean(TraderTaskRefreshPublisher.class)
    public TraderTaskRefreshPublisher traderTaskRefreshPublisher(StringRedisTemplate stringRedisTemplate) {
        return new TraderTaskRefreshPublisher(stringRedisTemplate);
    }
}
