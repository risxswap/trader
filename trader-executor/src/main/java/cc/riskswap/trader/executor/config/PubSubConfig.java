package cc.riskswap.trader.executor.config;

import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import cc.riskswap.trader.executor.pubsub.subscriber.BaseSubscriber;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis Pub/Sub Configuration
 */
@Slf4j
@Configuration
public class PubSubConfig {

    @SuppressWarnings("null")
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            Collection<BaseSubscriber> subscribes) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Use String serializer for topics
        container.setTopicSerializer(new StringRedisSerializer());

        for (BaseSubscriber subscribe : subscribes) {
            String channel = subscribe.getChannel();
            
            // Create adapter that delegates to processMessage(String)
            MessageListenerAdapter adapter = new MessageListenerAdapter(subscribe, "processMessage");
            // Set serializer to convert message body from bytes to String
            adapter.setSerializer(new StringRedisSerializer());
            adapter.afterPropertiesSet();
            
            container.addMessageListener(adapter, new ChannelTopic(channel));
            
            log.info("Registered Redis Pub/Sub listener for topic: {}", channel);
        }

        return container;
    }
}
