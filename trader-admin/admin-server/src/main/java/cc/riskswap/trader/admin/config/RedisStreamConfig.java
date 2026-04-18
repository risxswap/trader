package cc.riskswap.trader.admin.config;

import cc.riskswap.trader.admin.stream.TraderStreamConsumer;
import cc.riskswap.trader.base.event.TraderStreamPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class RedisStreamConfig {

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory, TraderStreamConsumer streamConsumer) {
            
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        try {
            connectionFactory.getConnection().streamCommands().xGroupCreate(
                    TraderStreamPublisher.STREAM_KEY.getBytes(), "trader-admin-group", ReadOffset.from("0-0"), true);
        } catch (Exception e) {
            // Group might already exist
        }

        listenerContainer.receive(
                Consumer.from("trader-admin-group", "admin-consumer"),
                StreamOffset.create(TraderStreamPublisher.STREAM_KEY, ReadOffset.lastConsumed()),
                streamConsumer);

        listenerContainer.start();
        return listenerContainer;
    }
}