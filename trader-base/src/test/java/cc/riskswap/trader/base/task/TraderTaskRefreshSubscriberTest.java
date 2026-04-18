package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TraderTaskRefreshSubscriberTest {

    @Test
    void should_log_when_task_trigger_message_is_received(CapturedOutput output) throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        TraderTaskSchedulerService schedulerService = mock(TraderTaskSchedulerService.class);
        TraderTaskRefreshSubscriber subscriber = new TraderTaskRefreshSubscriber("COLLECTOR", redisTemplate, schedulerService);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("trader:task:instances:COLLECTOR", "fundSync")).thenReturn("""
                {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金","enabled":true,"paramsJson":"{}"}
                """);

        subscriber.handle(new TraderTaskRefreshMessage("COLLECTOR", "fundSync", 3L, "TASK_TRIGGER"));

        verify(schedulerService).trigger(org.mockito.ArgumentMatchers.any());
        assertThat(output.getOut())
                .contains("Receive trader task refresh message")
                .contains("TASK_TRIGGER")
                .contains("fundSync");
    }
}
