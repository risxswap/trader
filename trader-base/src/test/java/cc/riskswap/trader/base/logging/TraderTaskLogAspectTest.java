package cc.riskswap.trader.base.logging;

import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

class TraderTaskLogAspectTest {

    @Test
    void should_skip_running_log_persistence_when_executor_manages_history() {
        TaskLogStore taskLogStore = Mockito.mock(TaskLogStore.class);
        TraderTaskLogAspect aspect = new TraderTaskLogAspect(taskLogStore);
        LocalDateTime startTime = LocalDateTime.parse("2026-04-19T10:30:00");

        TaskLogExecutionContext.markExecutorManaged(true);
        try {
            ReflectionTestUtils.invokeMethod(aspect, "persistRunningLog", "同步基金", "FundSyncTask", startTime, "trace-1");
        } finally {
            TaskLogExecutionContext.clear();
        }

        Mockito.verifyNoInteractions(taskLogStore);
    }

    @Test
    void should_delegate_running_log_persistence_to_task_log_store() {
        TaskLogStore taskLogStore = Mockito.mock(TaskLogStore.class);
        TraderTaskLogAspect aspect = new TraderTaskLogAspect(taskLogStore);
        LocalDateTime startTime = LocalDateTime.parse("2026-04-19T10:30:00");

        ReflectionTestUtils.invokeMethod(aspect, "persistRunningLog", "同步基金", "FundSyncTask", startTime, "trace-1");

        Mockito.verify(taskLogStore).writeRunning("同步基金", "FundSyncTask", startTime, "trace-1");
    }

    @Test
    void should_delegate_success_log_persistence_to_task_log_store() {
        TaskLogStore taskLogStore = Mockito.mock(TaskLogStore.class);
        TraderTaskLogAspect aspect = new TraderTaskLogAspect(taskLogStore);
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        Mockito.when(signature.toShortString()).thenReturn("FundSyncTask.execute()");
        LocalDateTime startTime = LocalDateTime.parse("2026-04-19T10:30:00");

        ReflectionTestUtils.invokeMethod(
                aspect,
                "persistSuccessLog",
                signature,
                "同步基金",
                "FundSyncTask",
                "trace-1",
                new Object[]{"arg"},
                false,
                "ok",
                startTime,
                1200L
        );

        Mockito.verify(taskLogStore).writeFinished(Mockito.eq("trace-1"), Mockito.eq("SUCCESS"), Mockito.eq(1200L), Mockito.contains("status: SUCCESS"));
    }
}
