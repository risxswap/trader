package cc.riskswap.trader.base.logging;

import cc.riskswap.trader.base.dao.TaskLogDao;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

class TaskLogStoreTest {

    @Test
    void should_delegate_running_log_write_to_task_log_dao() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TaskLogStore store = new TaskLogStore(taskLogDao);
        LocalDateTime startTime = LocalDateTime.parse("2026-04-19T10:15:30");

        store.writeRunning("同步基金", "fundSync", startTime, "trace-1");

        Mockito.verify(taskLogDao).createRunningLog("同步基金", "fundSync", startTime, "trace-1");
    }

    @Test
    void should_delegate_finished_log_write_to_task_log_dao() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TaskLogStore store = new TaskLogStore(taskLogDao);

        store.writeFinished("trace-1", "SUCCESS", 1200L, "done");

        Mockito.verify(taskLogDao).updateLogByTraceId("trace-1", "SUCCESS", 1200L, "done", null, null);
    }
}
