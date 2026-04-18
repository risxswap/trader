package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.ErrorCode;
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
import cc.riskswap.trader.base.dao.entity.Investment;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import cc.riskswap.trader.admin.exception.Warning;
import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import cc.riskswap.trader.base.task.TraderTaskRefreshMessage;
import cc.riskswap.trader.base.task.TraderTaskRefreshPublisher;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemTaskService {
    private final SystemTaskDao systemTaskDao;
    private final TraderTaskRefreshPublisher traderTaskRefreshPublisher;
    private final StringRedisTemplate stringRedisTemplate;
    private final InvestmentDao investmentDao;

    public PageDto<SystemTaskDto> list(SystemTaskListQuery query) {
        cc.riskswap.trader.base.dao.query.SystemTaskListQuery listQuery =
                BeanUtil.copyProperties(query, cc.riskswap.trader.base.dao.query.SystemTaskListQuery.class);
        Page<SystemTask> page = systemTaskDao.pageQuery(listQuery);

        PageDto<SystemTaskDto> result = new PageDto<>();
        result.setTotal(page.getTotal());
        result.setPageNo((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        List<SystemTaskDto> items = page.getRecords().stream()
                .map(item -> {
                    SystemTaskDto dto = BeanUtil.copyProperties(item, SystemTaskDto.class);
                    dto.setSourceType("SYSTEM");
                    return dto;
                })
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(query.getIncludeInvestment())) {
            cc.riskswap.trader.base.dao.query.InvestmentListQuery invQuery = new cc.riskswap.trader.base.dao.query.InvestmentListQuery();
            invQuery.setPageNo(1);
            invQuery.setPageSize(100);
            if (StrUtil.isNotBlank(query.getTaskName())) {
                invQuery.setName(query.getTaskName());
            }
            if (StrUtil.isNotBlank(query.getStatus())) {
                invQuery.setStatus(query.getStatus());
            }
            Page<Investment> invPage = investmentDao.pageQuery(invQuery);
            List<SystemTaskDto> invTasks = invPage.getRecords().stream().map(inv -> {
                SystemTaskDto dto = new SystemTaskDto();
                dto.setId(inv.getId().longValue());
                dto.setAppName("EXECUTOR");
                dto.setTaskType("EXECUTOR");
                dto.setSourceType("INVESTMENT");
                dto.setTaskCode("investment-" + inv.getId());
                dto.setTaskName(inv.getName());
                dto.setCron(inv.getCron());
                dto.setStatus(inv.getStatus());
                dto.setUpdatedAt(inv.getUpdatedAt());
                dto.setCreatedAt(inv.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());
            items.addAll(invTasks);
            result.setTotal(result.getTotal() + invPage.getTotal());
        }

        result.setItems(items);
        return result;
    }

    public List<TaskDefinitionDto> definitions(TaskDefinitionListQuery query) {
        Set<String> keys = stringRedisTemplate.keys("trader:task:def:*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(key -> stringRedisTemplate.opsForValue().get(key))
                .filter(StrUtil::isNotBlank)
                .map(json -> JSONUtil.toBean(json, TaskDefinitionDto.class))
                .filter(item -> query.getTaskType() == null || query.getTaskType().equals(item.getTaskType()))
                .filter(item -> query.getTaskCode() == null || item.getTaskCode().contains(query.getTaskCode()))
                .filter(item -> query.getTaskName() == null || (item.getTaskName() != null && item.getTaskName().contains(query.getTaskName())))
                .sorted(Comparator.comparing(TaskDefinitionDto::getTaskType).thenComparing(TaskDefinitionDto::getTaskCode))
                .toList();
    }

    public void createInstance(SystemTaskInstanceCreateParam param) {
        String key = "trader:task:def:" + param.getTaskType() + ":" + param.getTaskCode();
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "任务定义不存在");
        }
        TaskDefinitionDto def = JSONUtil.toBean(json, TaskDefinitionDto.class);

        SystemTask existing = systemTaskDao.getByAppNameAndTaskCode(param.getTaskType(), param.getTaskCode());
        if (existing != null) {
            throw new Warning(ErrorCode.PARAM_INVALID.code(), "任务实例已存在");
        }

        SystemTask task = new SystemTask();
        task.setAppName(param.getTaskType());
        task.setTaskType(param.getTaskType());
        task.setTaskCode(param.getTaskCode());
        task.setTaskName(param.getTaskName());
        task.setCron(param.getCron());
        task.setEnabled(param.getEnabled() != null ? param.getEnabled() : def.getDefaultEnabled());
        task.setStatus(param.getStatus());
        task.setParamSchema(def.getParamSchema());
        task.setDefaultParamsJson(def.getDefaultParamsJson());
        task.setParamsJson(StrUtil.isNotBlank(param.getParamsJson()) ? param.getParamsJson() : def.getDefaultParamsJson());
        task.setRemark(param.getRemark());
        task.setVersion(System.currentTimeMillis());
        systemTaskDao.save(task);

        publishRefresh(task, "TASK_CREATED");
    }

    public void deleteInstance(SystemTaskInstanceDeleteParam param) {
        SystemTask task = requireTask(param.getId());
        systemTaskDao.removeById(param.getId());
        publishRefresh(task, "TASK_DELETED");
    }

    public void update(SystemTaskUpdateParam param) {
        SystemTask task = requireTask(param.getId());
        task.setCron(param.getCron());
        if (param.getEnabled() != null) {
            task.setEnabled(param.getEnabled());
        }
        if (param.getStatus() != null) {
            task.setStatus(param.getStatus());
        }
        task.setParamsJson(param.getParamsJson());
        task.setRemark(param.getRemark());
        task.setVersion(System.currentTimeMillis());
        task.setUpdatedAt(OffsetDateTime.now());
        systemTaskDao.updateById(task);
        publishRefresh(task, "TASK_UPDATED");
    }

    public void trigger(SystemTaskTriggerParam param) {
        SystemTask task = requireTask(param.getId());
        publishRefresh(task, "TASK_TRIGGER");
    }

    public SystemTaskDto getDetail(Long id) {
        SystemTask task = requireTask(id);
        SystemTaskDto dto = BeanUtil.copyProperties(task, SystemTaskDto.class);
        dto.setSourceType("SYSTEM");
        return dto;
    }

    private SystemTask requireTask(Long id) {
        SystemTask task = systemTaskDao.getById(id);
        if (task == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "任务不存在");
        }
        return task;
    }

    private void publishRefresh(SystemTask task, String eventType) {
        if (!"TASK_DELETED".equals(eventType)) {
            SystemTaskStatusEvent event = new SystemTaskStatusEvent();
            event.setAppName(task.getAppName());
            event.setTaskType(task.getTaskType());
            event.setTaskCode(task.getTaskCode());
            event.setTaskName(task.getTaskName());
            event.setEnabled(task.getEnabled());
            event.setStatus(task.getStatus());
            event.setResult(task.getResult());
            event.setCron(task.getCron());
            event.setVersion(task.getVersion());
            event.setParamsJson(task.getParamsJson());
            String key = "trader:task:instances:" + task.getTaskType();
            stringRedisTemplate.opsForHash().put(key, task.getTaskCode(), JSONUtil.toJsonStr(event));
        } else if ("TASK_DELETED".equals(eventType)) {
            String key = "trader:task:instances:" + task.getTaskType();
            stringRedisTemplate.opsForHash().delete(key, task.getTaskCode());
        }

        traderTaskRefreshPublisher.publish(new TraderTaskRefreshMessage(
                task.getTaskType(),
                task.getTaskCode(),
                task.getVersion(),
                eventType
        ));
    }
}
