package cc.riskswap.trader.base.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.SubscriptionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraderTaskRedisListenerContainerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TraderBaseAutoConfiguration.class,
                    TraderTaskAutoConfiguration.class
            ))
            .withBean(RedisConnectionFactory.class, () -> {
                RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
                RedisConnection connection = mock(RedisConnection.class);
                when(factory.getConnection()).thenReturn(connection);
                doAnswer(invocation -> {
                    MessageListener listener = invocation.getArgument(0);
                    SubscriptionListener subscriptionListener = (SubscriptionListener) listener;
                    byte[] pattern = invocation.getArgument(1);
                    subscriptionListener.onPatternSubscribed(pattern, 1L);
                    return null;
                }).when(connection).pSubscribe(any(MessageListener.class), any(byte[].class));
                return factory;
            })
            .withPropertyValues(
                    "trader.redis.enabled=true",
                    "trader.node.type=collector",
                    "trader.task.refresh-poll-ms=999999999"
            );

    @Test
    void should_register_task_refresh_listener_container_when_task_and_redis_are_enabled() {
        contextRunner.run(context -> assertThat(context).hasBean("traderTaskRedisListenerContainer"));
    }
}
