package cc.riskswap.trader.admin.test.stream;

import cc.riskswap.trader.admin.stream.TraderStreamConsumer;
import cc.riskswap.trader.base.dao.NodeDao;
import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.TaskLogDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.util.Map;

class TraderStreamConsumerTest {

    @Test
    void should_create_running_task_log_from_task_log_event() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderStreamConsumer consumer = new TraderStreamConsumer(taskLogDao, nodeDao, nodeMonitorDao, systemTaskDao);

        MapRecord<String, String, String> record = MapRecord.create(
                "trader:stream",
                Map.of(
                        "eventType", "TASK_LOG",
                        "payload", """
                                {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金","traceId":"trace-1","status":"RUNNING"}
                                """
                )
        ).withId(RecordId.of("1-0"));

        consumer.onMessage(record);

        Mockito.verify(taskLogDao).createRunningLog(
                Mockito.eq("同步基金"),
                Mockito.eq("fundSync"),
                Mockito.any(),
                Mockito.eq("trace-1")
        );
    }

    @Test
    void should_update_success_task_log_by_trace_id() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderStreamConsumer consumer = new TraderStreamConsumer(taskLogDao, nodeDao, nodeMonitorDao, systemTaskDao);

        MapRecord<String, String, String> record = MapRecord.create(
                "trader:stream",
                Map.of(
                        "eventType", "TASK_LOG",
                        "payload", """
                                {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金","traceId":"trace-1","status":"SUCCESS","costMs":1200,"remark":"done"}
                                """
                )
        ).withId(RecordId.of("2-0"));

        consumer.onMessage(record);

        Mockito.verify(taskLogDao).updateLogByTraceId("trace-1", "SUCCESS", 1200L, "done");
    }

    @Test
    void should_update_failed_task_log_by_trace_id() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderStreamConsumer consumer = new TraderStreamConsumer(taskLogDao, nodeDao, nodeMonitorDao, systemTaskDao);

        MapRecord<String, String, String> record = MapRecord.create(
                "trader:stream",
                Map.of(
                        "eventType", "TASK_LOG",
                        "payload", """
                                {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金","traceId":"trace-1","status":"FAILED","costMs":1200,"remark":"boom"}
                                """
                )
        ).withId(RecordId.of("3-0"));

        consumer.onMessage(record);

        Mockito.verify(taskLogDao).updateLogByTraceId("trace-1", "FAILED", 1200L, "boom");
    }

    @Test
    void should_update_task_status_and_result_from_stream_event() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        NodeDao nodeDao = Mockito.mock(NodeDao.class);
        NodeMonitorDao nodeMonitorDao = Mockito.mock(NodeMonitorDao.class);
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderStreamConsumer consumer = new TraderStreamConsumer(taskLogDao, nodeDao, nodeMonitorDao, systemTaskDao);

        SystemTask task = new SystemTask();
        task.setId(1L);
        task.setTaskType("COLLECTOR");
        task.setTaskCode("collector.calendar.sync");
        task.setStatus("RUNNING");
        Mockito.when(systemTaskDao.getByTaskTypeAndTaskCode("COLLECTOR", "collector.calendar.sync")).thenReturn(task);

        MapRecord<String, String, String> record = MapRecord.create(
                "trader:stream",
                Map.of(
                        "eventType", "SYSTEM_TASK_STATUS",
                        "payload", """
                                {"taskType":"COLLECTOR","taskCode":"collector.calendar.sync","status":"STOPPED","result":"SUCCESS","version":5}
                                """
                )
        ).withId(RecordId.of("1-0"));

        consumer.onMessage(record);

        ArgumentCaptor<SystemTask> captor = ArgumentCaptor.forClass(SystemTask.class);
        Mockito.verify(systemTaskDao).updateById(captor.capture());
        SystemTask updated = captor.getValue();
        Assertions.assertEquals("STOPPED", updated.getStatus());
        Assertions.assertEquals("SUCCESS", updated.getResult());
        Assertions.assertEquals(5L, updated.getVersion());
    }
}
