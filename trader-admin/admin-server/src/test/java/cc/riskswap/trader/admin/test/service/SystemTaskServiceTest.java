package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.SystemTaskDto;
import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceCreateParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceDeleteParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskTriggerParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam;
import cc.riskswap.trader.admin.common.model.query.SystemTaskListQuery;
import cc.riskswap.trader.admin.dao.InvestmentDao;
import cc.riskswap.trader.admin.dao.SystemTaskDao;
import cc.riskswap.trader.admin.dao.entity.SystemTask;
import cc.riskswap.trader.admin.service.SystemTaskService;
import cc.riskswap.trader.base.task.TraderTaskRefreshMessage;
import cc.riskswap.trader.base.task.TraderTaskRefreshPublisher;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.util.List;

public class SystemTaskServiceTest {

    @Test
    void should_list_tasks_as_page_dto() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, refreshPublisher, stringRedisTemplate, investmentDao);

        SystemTask task = sampleTask();
        Page<SystemTask> page = new Page<>(2, 5);
        page.setTotal(9);
        page.setRecords(List.of(task));
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(page);

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(2);
        query.setPageSize(5);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertEquals(9, result.getTotal());
        Assertions.assertEquals(2, result.getPageNo());
        Assertions.assertEquals(5, result.getPageSize());
        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("fundSync", result.getItems().get(0).getTaskCode());
        Assertions.assertEquals("{\"fullSync\":true}", result.getItems().get(0).getParamsJson());
    }

    @Test
    void should_update_task_params_and_publish_task_updated_message() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, refreshPublisher, stringRedisTemplate, investmentDao);

        SystemTask task = sampleTask();
        task.setVersion(7L);
        Mockito.when(systemTaskDao.getById(1L)).thenReturn(task);

        SystemTaskUpdateParam param = new SystemTaskUpdateParam();
        param.setId(1L);
        param.setCron("0 */5 * * * ?");
        param.setStatus("STOPPED");
        param.setParamsJson("{\"fullSync\":false}");
        param.setRemark("暂停观察");

        systemTaskService.update(param);

        ArgumentCaptor<SystemTask> taskCaptor = ArgumentCaptor.forClass(SystemTask.class);
        Mockito.verify(systemTaskDao).updateById(taskCaptor.capture());
        SystemTask updated = taskCaptor.getValue();
        Assertions.assertEquals("0 */5 * * * ?", updated.getCron());
        Assertions.assertEquals("STOPPED", updated.getStatus());
        Assertions.assertEquals("{\"fullSync\":false}", updated.getParamsJson());
        Assertions.assertEquals("暂停观察", updated.getRemark());
        Assertions.assertNotNull(updated.getVersion());
        Assertions.assertTrue(updated.getVersion() > 7L);
        Assertions.assertNotNull(updated.getUpdatedAt());
        Mockito.verify(hashOperations).put(Mockito.eq("trader:task:instances:COLLECTOR"), Mockito.eq("fundSync"), Mockito.anyString());

        ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
        Mockito.verify(refreshPublisher).publish(messageCaptor.capture());
        TraderTaskRefreshMessage message = messageCaptor.getValue();
        Assertions.assertEquals("COLLECTOR", message.taskType());
        Assertions.assertEquals("fundSync", message.taskCode());
        Assertions.assertEquals(updated.getVersion(), message.version());
        Assertions.assertEquals("TASK_UPDATED", message.eventType());
    }

    @Test
    void should_publish_task_trigger_message_when_triggering_task() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, refreshPublisher, stringRedisTemplate, investmentDao);

        SystemTask task = sampleTask();
        task.setVersion(3L);
        Mockito.when(systemTaskDao.getById(1L)).thenReturn(task);

        SystemTaskTriggerParam param = new SystemTaskTriggerParam();
        param.setId(1L);

        systemTaskService.trigger(param);

        ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
        Mockito.verify(refreshPublisher).publish(messageCaptor.capture());
        TraderTaskRefreshMessage message = messageCaptor.getValue();
        Assertions.assertEquals("COLLECTOR", message.taskType());
        Assertions.assertEquals("fundSync", message.taskCode());
        Assertions.assertEquals(3L, message.version());
        Assertions.assertEquals("TASK_TRIGGER", message.eventType());
    }

    @Test
    void should_create_instance_from_definition_and_publish_created_message() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, refreshPublisher, stringRedisTemplate, investmentDao);

        String json = "{\"taskType\":\"COLLECTOR\",\"taskCode\":\"fundSync\",\"taskName\":\"同步基金\",\"defaultCron\":\"0 0 1 * * ?\",\"defaultEnabled\":true}";
        Mockito.when(ops.get("trader:task:def:COLLECTOR:fundSync")).thenReturn(json);

        SystemTaskInstanceCreateParam param = new SystemTaskInstanceCreateParam();
        param.setTaskType("COLLECTOR");
        param.setTaskCode("fundSync");
        param.setTaskName("自定义同步基金");
        param.setCron("0 15 9 * * ?");
        param.setStatus("STOPPED");
        param.setParamsJson("{\"fullSync\":false}");
        param.setRemark("手工创建");

        systemTaskService.createInstance(param);

        ArgumentCaptor<SystemTask> savedTaskCaptor = ArgumentCaptor.forClass(SystemTask.class);
        Mockito.verify(systemTaskDao).save(savedTaskCaptor.capture());
        SystemTask savedTask = savedTaskCaptor.getValue();
        Assertions.assertEquals("自定义同步基金", savedTask.getTaskName());
        Assertions.assertEquals("0 15 9 * * ?", savedTask.getCron());
        Assertions.assertEquals("STOPPED", savedTask.getStatus());
        Assertions.assertEquals("{\"fullSync\":false}", savedTask.getParamsJson());
        Assertions.assertEquals("手工创建", savedTask.getRemark());
        Mockito.verify(hashOperations).put(Mockito.eq("trader:task:instances:COLLECTOR"), Mockito.eq("fundSync"), Mockito.anyString());

        ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
        Mockito.verify(refreshPublisher).publish(messageCaptor.capture());
        TraderTaskRefreshMessage message = messageCaptor.getValue();
        Assertions.assertEquals("COLLECTOR", message.taskType());
        Assertions.assertEquals("fundSync", message.taskCode());
        Assertions.assertEquals("TASK_CREATED", message.eventType());
    }

    @Test
    void should_delete_instance_and_publish_deleted_message() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, refreshPublisher, stringRedisTemplate, investmentDao);

        SystemTask task = sampleTask();
        Mockito.when(systemTaskDao.getById(1L)).thenReturn(task);

        SystemTaskInstanceDeleteParam param = new SystemTaskInstanceDeleteParam();
        param.setId(1L);

        systemTaskService.deleteInstance(param);

        Mockito.verify(hashOperations).delete("trader:task:instances:COLLECTOR", "fundSync");
        ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
        Mockito.verify(refreshPublisher).publish(messageCaptor.capture());
        TraderTaskRefreshMessage message = messageCaptor.getValue();
        Assertions.assertEquals("COLLECTOR", message.taskType());
        Assertions.assertEquals("fundSync", message.taskCode());
        Assertions.assertEquals("TASK_DELETED", message.eventType());
    }

    private SystemTask sampleTask() {
        SystemTask task = new SystemTask();
        task.setId(1L);
        task.setAppName("trader-collector");
        task.setTaskType("COLLECTOR");
        task.setTaskCode("fundSync");
        task.setTaskName("同步基金");
        task.setCron("0 0 1 * * ?");
        task.setStatus("RUNNING");
        task.setParamsJson("{\"fullSync\":true}");
        task.setUpdatedAt(OffsetDateTime.parse("2026-04-15T10:00:00+08:00"));
        return task;
    }
}
