package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.TaskLog;
import cc.riskswap.trader.base.dao.mapper.TaskLogMapper;
import cc.riskswap.trader.base.dao.query.TaskLogListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TaskLogDao extends ServiceImpl<TaskLogMapper, TaskLog> {
    public Page<TaskLog> pageQuery(TaskLogListQuery query) {
        Page<TaskLog> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTaskCode())) {
            wrapper.eq(TaskLog::getTaskGroup, query.getTaskCode());
        }
        if (StrUtil.isNotBlank(query.getTaskName())) {
            wrapper.like(TaskLog::getTaskName, query.getTaskName());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(TaskLog::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(TaskLog::getStartTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(TaskLog::getStartTime, query.getEndTime());
        }
        wrapper.orderByDesc(TaskLog::getStartTime);
        return this.page(page, wrapper);
    }

    public void createRunningLog(String taskName, String taskGroup, java.time.LocalDateTime startTime, String traceId) {
        TaskLog log = new TaskLog();
        log.setTaskName(taskName);
        log.setTaskGroup(taskGroup);
        log.setStartTime(java.time.OffsetDateTime.of(startTime, java.time.ZoneOffset.UTC));
        log.setTraceId(traceId);
        log.setStatus("RUNNING");
        this.save(log);
    }

    public void updateLogByTraceId(String traceId, String status, Long costMs, String remark) {
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskLog::getTraceId, traceId);
        TaskLog log = this.getOne(wrapper, false);
        if (log != null) {
            log.setStatus(status);
            log.setExecutionMs(costMs);
            log.setRemark(remark);
            log.setEndTime(java.time.OffsetDateTime.now());
            this.updateById(log);
        }
    }

    public Map<String, Long> countByTaskGroups(List<String> taskGroups) {
        if (taskGroups == null || taskGroups.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<TaskLog> wrapper = new QueryWrapper<>();
        wrapper.select("task_group", "COUNT(*) AS execution_count");
        wrapper.in("task_group", taskGroups);
        wrapper.groupBy("task_group");
        List<Map<String, Object>> rows = this.listMaps(wrapper);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object taskGroup = row.get("task_group");
            Object executionCount = row.get("execution_count");
            if (taskGroup != null && executionCount instanceof Number number) {
                result.put(String.valueOf(taskGroup), number.longValue());
            }
        }
        return result;
    }

    public Map<String, Long> latestExecutionMsByTaskGroups(List<String> taskGroups) {
        if (taskGroups == null || taskGroups.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(TaskLog::getTaskGroup, TaskLog::getExecutionMs, TaskLog::getStartTime);
        wrapper.in(TaskLog::getTaskGroup, taskGroups);
        wrapper.orderByDesc(TaskLog::getStartTime);
        List<TaskLog> logs = this.list(wrapper);
        Map<String, Long> result = new LinkedHashMap<>();
        for (TaskLog log : logs) {
            if (StrUtil.isBlank(log.getTaskGroup()) || result.containsKey(log.getTaskGroup())) {
                continue;
            }
            result.put(log.getTaskGroup(), log.getExecutionMs());
        }
        return result;
    }
}
