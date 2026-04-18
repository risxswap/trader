package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.task.TraderTaskRefreshSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "trader.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TraderRedisProperties.class)
public class TraderRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${trader.redis.host:}')")
    public RedisConnectionFactory redisConnectionFactory(TraderRedisProperties traderRedisProperties) {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(traderRedisProperties.getHost());
        configuration.setPort(traderRedisProperties.getPort());
        configuration.setDatabase(traderRedisProperties.getDatabase());
        if (StringUtils.hasText(traderRedisProperties.getPassword())) {
            configuration.setPassword(traderRedisProperties.getPassword());
        }
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnBean({RedisConnectionFactory.class, TraderTaskRefreshSubscriber.class})
    @ConditionalOnMissingBean(name = "traderTaskRedisListenerContainer")
    public RedisMessageListenerContainer traderTaskRedisListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            TraderTaskRefreshSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handle");
        adapter.setSerializer(new StringRedisSerializer());
        adapter.afterPropertiesSet();
        container.addMessageListener(adapter, new PatternTopic("trader:task:refresh"));
        return container;
    }
}
