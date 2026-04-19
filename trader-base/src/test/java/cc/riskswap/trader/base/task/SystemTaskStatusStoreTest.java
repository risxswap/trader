package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemTaskStatusStoreTest {

    @Test
    void should_update_system_task_status_and_result_by_task_type_and_code() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        SystemTask task = new SystemTask();
        task.setId(10L);
        task.setTaskType("COLLECTOR");
        task.setTaskCode("fundSync");
        task.setStatus("STOPPED");
        task.setResult(null);
        task.setVersion(1L);
        task.setUpdatedAt(OffsetDateTime.parse("2026-04-19T10:00:00Z"));
        Mockito.when(systemTaskDao.getByTaskTypeAndTaskCode("COLLECTOR", "fundSync")).thenReturn(task);

        SystemTaskStatusStore store = new SystemTaskStatusStore(systemTaskDao);

        store.writeStatus("COLLECTOR", "fundSync", "RUNNING", null, 5L);

        ArgumentCaptor<SystemTask> captor = ArgumentCaptor.forClass(SystemTask.class);
        Mockito.verify(systemTaskDao).updateById(captor.capture());
        SystemTask updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("RUNNING", updated.getStatus());
        assertEquals(null, updated.getResult());
        assertEquals(5L, updated.getVersion());
    }
}
