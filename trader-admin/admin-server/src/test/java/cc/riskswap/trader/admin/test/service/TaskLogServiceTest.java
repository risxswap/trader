package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.TaskLogDto;
import cc.riskswap.trader.admin.common.model.query.TaskLogQuery;
import cc.riskswap.trader.admin.dao.TaskLogDao;
import cc.riskswap.trader.admin.dao.entity.TaskLog;
import cc.riskswap.trader.admin.service.TaskLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

class TaskLogServiceTest {

    @Test
    void should_query_task_logs_by_task_code_and_task_name() {
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TaskLogService taskLogService = new TaskLogService(taskLogDao);

        TaskLog taskLog = new TaskLog();
        taskLog.setId(1L);
        taskLog.setTaskName("同步基金");
        taskLog.setTaskGroup("fundSync");
        taskLog.setTraceId("trace-1");
        taskLog.setStatus("SUCCESS");
        taskLog.setExecutionMs(800L);
        taskLog.setCreatedAt(OffsetDateTime.parse("2026-04-19T01:00:00+08:00"));

        Page<TaskLog> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(taskLog));
        Mockito.when(taskLogDao.pageQuery(Mockito.any())).thenReturn(page);

        TaskLogQuery query = new TaskLogQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        query.setTaskCode("fundSync");
        query.setTaskName("同步基金");

        PageDto<TaskLogDto> result = taskLogService.list(query);

        ArgumentCaptor<cc.riskswap.trader.admin.dao.query.TaskLogListQuery> captor =
                ArgumentCaptor.forClass(cc.riskswap.trader.admin.dao.query.TaskLogListQuery.class);
        Mockito.verify(taskLogDao).pageQuery(captor.capture());
        cc.riskswap.trader.admin.dao.query.TaskLogListQuery actualQuery = captor.getValue();
        Assertions.assertEquals("fundSync", actualQuery.getTaskCode());
        Assertions.assertEquals("同步基金", actualQuery.getTaskName());
        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("trace-1", result.getItems().get(0).getTraceId());
    }
}
