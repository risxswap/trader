package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.logging.TaskLogStore;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TraderTaskExecutorTest {

    @Test
    void should_log_when_triggered_task_executes(CapturedOutput output) throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        TraderTaskLock traderTaskLock = mock(TraderTaskLock.class);
        SystemTaskStatusStore statusStore = mock(SystemTaskStatusStore.class);
        TaskLogStore taskLogStore = mock(TaskLogStore.class);
        SampleTask task = new SampleTask();
        TraderTaskRegistry registry = new TraderTaskRegistry(List.of(task));
        TraderTaskExecutor executor = new TraderTaskExecutor(registry, redisTemplate, traderTaskLock, statusStore, taskLogStore);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("trader:task:instances:COLLECTOR", "fundSync")).thenReturn("""
                {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金","enabled":true,"paramsJson":"{}"}
                """);
        when(traderTaskLock.newRequestId()).thenReturn("req-1");
        when(traderTaskLock.tryLock("task:run:COLLECTOR:fundSync:1710000000", "req-1")).thenReturn(true);

        executor.execute("COLLECTOR", "fundSync", 1710000000L);

        verify(statusStore).writeStatus("COLLECTOR", "fundSync", "RUNNING", null, null);
        verify(statusStore).writeStatus("COLLECTOR", "fundSync", "STOPPED", "SUCCESS", null);
        verify(taskLogStore).writeRunning(eq("同步基金"), eq("fundSync"), any(), anyString());
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskLogStore).writeFinished(anyString(), eq("SUCCESS"), anyLong(), contains("triggerType=SCHEDULED"), contentCaptor.capture(), isNull());
        org.assertj.core.api.Assertions.assertThat(JSONUtil.parseObj(contentCaptor.getValue()).getLong("syncedCount")).isEqualTo(10L);
        org.assertj.core.api.Assertions.assertThat(JSONUtil.parseObj(contentCaptor.getValue()).getLong("failedCount")).isEqualTo(2L);
        org.assertj.core.api.Assertions.assertThat(JSONUtil.parseObj(contentCaptor.getValue()).getStr("message")).isEqualTo("done");
        org.assertj.core.api.Assertions.assertThat(output.getOut())
                .contains("Trigger trader task execution")
                .contains("fundSync");
    }

    @Test
    void should_publish_failed_result_when_task_execution_throws() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        TraderTaskLock traderTaskLock = mock(TraderTaskLock.class);
        SystemTaskStatusStore statusStore = mock(SystemTaskStatusStore.class);
        TaskLogStore taskLogStore = mock(TaskLogStore.class);
        FailingTask task = new FailingTask();
        TraderTaskRegistry registry = new TraderTaskRegistry(List.of(task));
        TraderTaskExecutor executor = new TraderTaskExecutor(registry, redisTemplate, traderTaskLock, statusStore, taskLogStore);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("trader:task:instances:COLLECTOR", "fundSync")).thenReturn("""
                {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金","enabled":true,"paramsJson":"{}"}
                """);
        when(traderTaskLock.newRequestId()).thenReturn("req-1");
        when(traderTaskLock.tryLock("task:run:COLLECTOR:fundSync:1710000000", "req-1")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> executor.execute("COLLECTOR", "fundSync", 1710000000L));

        verify(statusStore).writeStatus("COLLECTOR", "fundSync", "RUNNING", null, null);
        verify(statusStore).writeStatus("COLLECTOR", "fundSync", "STOPPED", "FAILED", null);
        verify(taskLogStore).writeRunning(eq("同步基金"), eq("fundSync"), any(), anyString());
        ArgumentCaptor<String> failedContentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskLogStore).writeFinished(anyString(), eq("FAILED"), anyLong(), contains("triggerType=SCHEDULED"), failedContentCaptor.capture(), errorMsgCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(JSONUtil.parseObj(failedContentCaptor.getValue()).getLong("syncedCount")).isEqualTo(3L);
        org.assertj.core.api.Assertions.assertThat(JSONUtil.parseObj(failedContentCaptor.getValue()).getLong("failedCount")).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(errorMsgCaptor.getValue()).contains("boom");
    }

    private static class SampleTask implements CollectorTask {
        @Override
        public String getTaskCode() {
            return "fundSync";
        }

        @Override
        public String getTaskName() {
            return "同步基金";
        }

        @Override
        public boolean defaultEnabled() {
            return true;
        }

        @Override
        public String getParamSchema() {
            return "{\"type\":\"object\"}";
        }

        @Override
        public String getDefaultParams() {
            return "{}";
        }

        @Override
        public void execute(TraderTaskContext context) {
            context.report().addSynced(10);
            context.report().addFailed(2);
            context.report().setMessage("done");
        }
    }

    private static class FailingTask extends SampleTask {
        @Override
        public void execute(TraderTaskContext context) {
            context.report().addSynced(3);
            context.report().addFailed(1);
            throw new IllegalStateException("boom");
        }
    }
}
