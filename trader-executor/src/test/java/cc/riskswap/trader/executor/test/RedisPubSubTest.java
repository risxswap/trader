package cc.riskswap.trader.executor.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import cc.riskswap.trader.executor.Application;
import cc.riskswap.trader.executor.pubsub.subscriber.BaseSubscriber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = {Application.class, RedisPubSubTest.TestConfig.class})
public class RedisPubSubTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CountDownLatch latch;

    @Test
    public void testPubSub() throws InterruptedException {
        String topic = "test:topic";
        String message = "hello world";

        // Publish message
        stringRedisTemplate.convertAndSend(topic, message);
        log.info("Published message to topic: {}, content: {}", topic, message);

        // Wait for handler to process
        boolean processed = latch.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(processed, "Message should be processed by handler");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public CountDownLatch latch() {
            return new CountDownLatch(1);
        }

        @Bean
        public BaseSubscriber testMsgHandler(CountDownLatch latch) {
            return new BaseSubscriber() {
                @Override
                public String getChannel() {
                    return "test:topic";
                }

                @Override
                public void processMessage(String message) {
                    log.info("Received message in test handler: {}", message);
                    if ("hello world".equals(message)) {
                        latch.countDown();
                    }
                }
            };
        }
    }
}
