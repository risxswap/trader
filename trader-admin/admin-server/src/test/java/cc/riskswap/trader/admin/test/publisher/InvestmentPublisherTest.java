package cc.riskswap.trader.admin.test.publisher;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import cc.riskswap.trader.admin.pubsub.publisher.BasePublisher;
import cc.riskswap.trader.admin.pubsub.publisher.InvestmentPublisher;

public class InvestmentPublisherTest {

    @Test
    public void testPublish() throws Exception {
        InvestmentPublisher publisher = new InvestmentPublisher();

        Field f = BasePublisher.class.getDeclaredField("stringRedisTemplate");
        f.setAccessible(true);

        publisher.create(1);
    }
}
