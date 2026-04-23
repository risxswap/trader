package cc.riskswap.trader.admin.test.service;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.SystemTaskDto;
import cc.riskswap.trader.admin.common.model.dto.TaskDefinitionDto;
import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceCreateParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceDeleteParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskTriggerParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam;
import cc.riskswap.trader.admin.common.model.query.SystemTaskListQuery;
import cc.riskswap.trader.admin.common.model.query.TaskDefinitionListQuery;
import cc.riskswap.trader.base.dao.InvestmentDao;
import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.TaskLogDao;
import cc.riskswap.trader.base.dao.entity.Investment;
import cc.riskswap.trader.base.dao.entity.SystemTask;
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
import java.util.Map;
import java.util.Set;

public class SystemTaskServiceTest {

    private static final String SAMPLE_PARAM_SCHEMA = "{\"type\":\"object\",\"properties\":{\"fullSync\":{\"type\":\"boolean\"}}}";
    private static final String SAMPLE_DEFAULT_PARAMS_JSON = "{\"fullSync\":true}";

    @Test
    void should_fill_last_execution_ms_from_latest_task_log() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        Page<SystemTask> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(sampleTask()));
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(page);
        Mockito.when(taskLogDao.countByTaskGroups(List.of("fundSync"))).thenReturn(Map.of("fundSync", 3L));
        Mockito.when(taskLogDao.latestExecutionMsByTaskGroups(List.of("fundSync"))).thenReturn(Map.of("fundSync", 65000L));

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(1);
        query.setPageSize(10);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertEquals(65000L, result.getItems().get(0).getLastExecutionMs());
    }

    @Test
    void should_leave_last_execution_ms_null_when_latest_log_has_no_duration() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        Page<SystemTask> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(sampleTask()));
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(page);
        Mockito.when(taskLogDao.countByTaskGroups(List.of("fundSync"))).thenReturn(Map.of("fundSync", 3L));
        Mockito.when(taskLogDao.latestExecutionMsByTaskGroups(List.of("fundSync"))).thenReturn(Map.of());

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(1);
        query.setPageSize(10);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertNull(result.getItems().get(0).getLastExecutionMs());
    }

    @Test
    void should_leave_investment_last_execution_ms_null() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        Page<SystemTask> systemPage = new Page<>(1, 10);
        systemPage.setTotal(0);
        systemPage.setRecords(List.of());
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(systemPage);

        Investment investment = new Investment();
        investment.setId(11);
        investment.setName("趋势策略");
        investment.setCron("0 */5 * * * ?");
        investment.setStatus("RUNNING");
        investment.setUpdatedAt(OffsetDateTime.parse("2026-04-23T10:00:00+08:00"));
        investment.setCreatedAt(OffsetDateTime.parse("2026-04-23T09:00:00+08:00"));
        Page<Investment> investmentPage = new Page<>(1, 100);
        investmentPage.setTotal(1);
        investmentPage.setRecords(List.of(investment));
        Mockito.when(investmentDao.pageQuery(Mockito.any())).thenReturn(investmentPage);

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        query.setIncludeInvestment(true);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("INVESTMENT", result.getItems().get(0).getSourceType());
        Assertions.assertNull(result.getItems().get(0).getLastExecutionMs());
        Mockito.verifyNoInteractions(taskLogDao);
    }

    @Test
    void should_aggregate_execution_count_for_system_tasks() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        Page<SystemTask> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(sampleTask()));
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(page);
        Mockito.when(taskLogDao.countByTaskGroups(List.of("fundSync"))).thenReturn(Map.of("fundSync", 3L));

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(1);
        query.setPageSize(10);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertEquals(3L, result.getItems().get(0).getExecutionCount());
    }

    @Test
    void should_default_execution_count_to_zero_when_task_has_no_logs() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        Page<SystemTask> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(sampleTask()));
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(page);
        Mockito.when(taskLogDao.countByTaskGroups(List.of("fundSync"))).thenReturn(Map.of());

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(1);
        query.setPageSize(10);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertEquals(0L, result.getItems().get(0).getExecutionCount());
    }

    @Test
    void should_default_investment_execution_count_to_zero_without_log_aggregation() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        Page<SystemTask> systemPage = new Page<>(1, 10);
        systemPage.setTotal(0);
        systemPage.setRecords(List.of());
        Mockito.when(systemTaskDao.pageQuery(Mockito.any())).thenReturn(systemPage);

        Investment investment = new Investment();
        investment.setId(11);
        investment.setName("趋势策略");
        investment.setCron("0 */5 * * * ?");
        investment.setStatus("RUNNING");
        investment.setUpdatedAt(OffsetDateTime.parse("2026-04-23T10:00:00+08:00"));
        investment.setCreatedAt(OffsetDateTime.parse("2026-04-23T09:00:00+08:00"));
        Page<Investment> investmentPage = new Page<>(1, 100);
        investmentPage.setTotal(1);
        investmentPage.setRecords(List.of(investment));
        Mockito.when(investmentDao.pageQuery(Mockito.any())).thenReturn(investmentPage);

        SystemTaskListQuery query = new SystemTaskListQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        query.setIncludeInvestment(true);

        PageDto<SystemTaskDto> result = systemTaskService.list(query);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("INVESTMENT", result.getItems().get(0).getSourceType());
        Assertions.assertEquals(0L, result.getItems().get(0).getExecutionCount());
        Mockito.verifyNoInteractions(taskLogDao);
    }

    @Test
    void should_list_tasks_as_page_dto() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

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
        Assertions.assertEquals("SUCCESS", result.getItems().get(0).getResult());
        Assertions.assertEquals("{\"fullSync\":true}", result.getItems().get(0).getParamsJson());
    }

    @Test
    void should_get_task_detail() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        SystemTask task = sampleTask();
        Mockito.when(systemTaskDao.getById(1L)).thenReturn(task);

        SystemTaskDto detail = systemTaskService.getDetail(1L);

        Assertions.assertEquals(1L, detail.getId());
        Assertions.assertEquals("fundSync", detail.getTaskCode());
        Assertions.assertEquals("SUCCESS", detail.getResult());
    }

    @Test
    void should_update_task_params_and_publish_task_updated_message() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

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
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

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
        Mockito.verify(hashOperations).put(Mockito.eq("trader:task:instances:COLLECTOR"), Mockito.eq("fundSync"), Mockito.anyString());
    }

    @Test
    void should_read_param_schema_and_default_params_from_redis_definitions() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(stringRedisTemplate.keys("trader:task:def:*"))
                .thenReturn(Set.of("trader:task:def:COLLECTOR:fundSync"));
        Mockito.when(ops.get("trader:task:def:COLLECTOR:fundSync"))
                .thenReturn(definitionJson("COLLECTOR", "fundSync", "同步基金", "0 0 1 * * ?"));
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        List<TaskDefinitionDto> definitions = systemTaskService.definitions(new TaskDefinitionListQuery());

        Assertions.assertEquals(1, definitions.size());
        Assertions.assertEquals("fundSync", definitions.getFirst().getTaskCode());
        Assertions.assertEquals(SAMPLE_PARAM_SCHEMA, definitions.getFirst().getParamSchema());
        Assertions.assertEquals(SAMPLE_DEFAULT_PARAMS_JSON, definitions.getFirst().getDefaultParamsJson());
    }

    @Test
    void should_create_instance_from_definition_and_publish_created_message() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        String json = definitionJson("COLLECTOR", "fundSync", "同步基金", "0 0 1 * * ?");
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
        Assertions.assertEquals(SAMPLE_PARAM_SCHEMA, savedTask.getParamSchema());
        Assertions.assertEquals(SAMPLE_DEFAULT_PARAMS_JSON, savedTask.getDefaultParamsJson());
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
    void should_reject_duplicate_creation_for_non_strategy_task() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        String json = "{\"taskType\":\"COLLECTOR\",\"taskCode\":\"fundSync\",\"taskName\":\"同步基金\",\"defaultCron\":\"0 0 1 * * ?\",\"defaultEnabled\":true}";
        Mockito.when(ops.get("trader:task:def:COLLECTOR:fundSync")).thenReturn(json);
        Mockito.when(systemTaskDao.getByAppNameAndTaskCode("COLLECTOR", "fundSync")).thenReturn(sampleTask());

        SystemTaskInstanceCreateParam param = new SystemTaskInstanceCreateParam();
        param.setTaskType("COLLECTOR");
        param.setTaskCode("fundSync");
        param.setTaskName("自定义同步基金");
        param.setCron("0 15 9 * * ?");
        param.setStatus("STOPPED");

        cc.riskswap.trader.admin.exception.Warning warning = Assertions.assertThrows(
                cc.riskswap.trader.admin.exception.Warning.class,
                () -> systemTaskService.createInstance(param)
        );

        Assertions.assertEquals("任务实例已存在", warning.getMessage());
        Mockito.verify(systemTaskDao, Mockito.never()).save(Mockito.any());
        Mockito.verifyNoInteractions(refreshPublisher);
    }

    @Test
    void should_generate_instance_task_code_for_strategy_task_creation() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        String json = "{\"taskType\":\"STRATEGY\",\"taskCode\":\"relativeStrength\",\"taskName\":\"相对强弱策略\",\"defaultCron\":\"0 0/1 * * * ?\",\"defaultEnabled\":true}";
        Mockito.when(ops.get("trader:task:def:STRATEGY:relativeStrength")).thenReturn(json);

        SystemTaskInstanceCreateParam param = new SystemTaskInstanceCreateParam();
        param.setTaskType("STRATEGY");
        param.setTaskCode("relativeStrength");
        param.setTaskName("相对强弱策略实例");
        param.setCron("0 15 9 * * ?");
        param.setStatus("STOPPED");

        systemTaskService.createInstance(param);

        ArgumentCaptor<SystemTask> taskCaptor = ArgumentCaptor.forClass(SystemTask.class);
        Mockito.verify(systemTaskDao).save(taskCaptor.capture());
        SystemTask savedTask = taskCaptor.getValue();
        Assertions.assertTrue(savedTask.getTaskCode().startsWith("relativeStrength#"));
        Assertions.assertNotEquals("relativeStrength", savedTask.getTaskCode());

        ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
        Mockito.verify(refreshPublisher).publish(messageCaptor.capture());
        Assertions.assertEquals(savedTask.getTaskCode(), messageCaptor.getValue().taskCode());
        Mockito.verify(hashOperations).put(Mockito.eq("trader:task:instances:STRATEGY"), Mockito.eq(savedTask.getTaskCode()), Mockito.anyString());
    }

    @Test
    void should_create_distinct_instance_codes_for_repeated_strategy_creation() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

        String json = "{\"taskType\":\"STRATEGY\",\"taskCode\":\"relativeStrength\",\"taskName\":\"相对强弱策略\",\"defaultCron\":\"0 0/1 * * * ?\",\"defaultEnabled\":true}";
        Mockito.when(ops.get("trader:task:def:STRATEGY:relativeStrength")).thenReturn(json);

        SystemTaskInstanceCreateParam param = new SystemTaskInstanceCreateParam();
        param.setTaskType("STRATEGY");
        param.setTaskCode("relativeStrength");
        param.setTaskName("相对强弱策略实例");
        param.setCron("0 15 9 * * ?");
        param.setStatus("STOPPED");

        systemTaskService.createInstance(param);
        systemTaskService.createInstance(param);

        ArgumentCaptor<SystemTask> taskCaptor = ArgumentCaptor.forClass(SystemTask.class);
        Mockito.verify(systemTaskDao, Mockito.times(2)).save(taskCaptor.capture());
        List<SystemTask> savedTasks = taskCaptor.getAllValues();
        Assertions.assertEquals(2, savedTasks.size());
        Assertions.assertNotEquals(savedTasks.get(0).getTaskCode(), savedTasks.get(1).getTaskCode());
        Assertions.assertTrue(savedTasks.stream().allMatch(task -> task.getTaskCode().startsWith("relativeStrength#")));

        ArgumentCaptor<Object> fieldCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(hashOperations, Mockito.times(2))
                .put(Mockito.eq("trader:task:instances:STRATEGY"), fieldCaptor.capture(), Mockito.anyString());
        Assertions.assertEquals(2, Set.copyOf(fieldCaptor.getAllValues()).size());

        ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
        Mockito.verify(refreshPublisher, Mockito.times(2)).publish(messageCaptor.capture());
        List<TraderTaskRefreshMessage> messages = messageCaptor.getAllValues();
        Assertions.assertNotEquals(messages.get(0).taskCode(), messages.get(1).taskCode());
    }

    @Test
    void should_delete_instance_and_publish_deleted_message() {
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TaskLogDao taskLogDao = Mockito.mock(TaskLogDao.class);
        TraderTaskRefreshPublisher refreshPublisher = Mockito.mock(TraderTaskRefreshPublisher.class);
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        Mockito.when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        InvestmentDao investmentDao = Mockito.mock(InvestmentDao.class);
        SystemTaskService systemTaskService = new SystemTaskService(systemTaskDao, taskLogDao, refreshPublisher, stringRedisTemplate, investmentDao);

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
        task.setResult("SUCCESS");
        task.setParamsJson("{\"fullSync\":true}");
        task.setUpdatedAt(OffsetDateTime.parse("2026-04-15T10:00:00+08:00"));
        return task;
    }

    private String definitionJson(String taskType, String taskCode, String taskName, String defaultCron) {
        return "{\"taskType\":\"" + taskType + "\",\"taskCode\":\"" + taskCode
                + "\",\"taskName\":\"" + taskName + "\",\"defaultCron\":\"" + defaultCron + "\",\"defaultEnabled\":true,"
                + "\"paramSchema\":\"" + SAMPLE_PARAM_SCHEMA.replace("\"", "\\\"") + "\","
                + "\"defaultParamsJson\":\"" + SAMPLE_DEFAULT_PARAMS_JSON.replace("\"", "\\\"") + "\"}";
    }
}
