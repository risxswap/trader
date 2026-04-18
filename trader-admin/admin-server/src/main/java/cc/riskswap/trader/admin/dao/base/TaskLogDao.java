package cc.riskswap.trader.admin.dao.base;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.dao.base.entity.TaskLog;
import cc.riskswap.trader.admin.dao.base.mapper.TaskLogMapper;
import cc.riskswap.trader.admin.dao.base.query.TaskLogListQuery;
import cn.hutool.core.util.StrUtil;

/**
 * 任务执行日志DAO类
 */
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
        wrapper.orderByDesc(TaskLog::getCreatedAt);

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
}
