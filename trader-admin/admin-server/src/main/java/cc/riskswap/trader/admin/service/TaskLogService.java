package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.TaskLogDto;
import cc.riskswap.trader.admin.common.model.query.TaskLogQuery;
import cc.riskswap.trader.admin.exception.Warning;
import cc.riskswap.trader.base.dao.TaskLogDao;
import cc.riskswap.trader.base.dao.entity.TaskLog;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLogService {
    private final TaskLogDao taskLogDao;

    public PageDto<TaskLogDto> list(TaskLogQuery query) {
        cc.riskswap.trader.base.dao.query.TaskLogListQuery listQuery = new cc.riskswap.trader.base.dao.query.TaskLogListQuery();
        BeanUtil.copyProperties(query, listQuery);
        Page<TaskLog> page = taskLogDao.pageQuery(listQuery);

        PageDto<TaskLogDto> result = new PageDto<>();
        result.setTotal(page.getTotal());
        result.setPageNo((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        
        List<TaskLogDto> items = page.getRecords().stream()
            .map(item -> BeanUtil.copyProperties(item, TaskLogDto.class))
            .collect(Collectors.toList());
        result.setItems(items);
        
        return result;
    }

    public TaskLogDto getDetail(Long id) {
        TaskLog log = taskLogDao.getById(id);
        if (log == null) return null;
        return BeanUtil.copyProperties(log, TaskLogDto.class);
    }

    public void delete(Long id) {
        TaskLog log = taskLogDao.getById(id);
        if (log == null) {
            throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "执行历史不存在");
        }
        taskLogDao.removeById(id);
    }

    public Long startTask(String taskName, String taskGroup) {
        TaskLog taskLog = new TaskLog();
        taskLog.setTaskName(taskName);
        taskLog.setTaskGroup(taskGroup);
        taskLog.setStartTime(OffsetDateTime.now());
        taskLog.setStatus("RUNNING");
        taskLogDao.save(taskLog);
        return taskLog.getId();
    }

    public void appendContent(Long logId, String content) {
        if (logId == null || StrUtil.isBlank(content)) return;
        TaskLog logItem = taskLogDao.getById(logId);
        if (logItem != null) {
            String current = logItem.getContent() == null ? "" : logItem.getContent() + "\n";
            logItem.setContent(current + content);
            taskLogDao.updateById(logItem);
        }
    }

    public void finishTask(Long logId, boolean success, String content, String errorMsg) {
        if (logId == null) return;
        TaskLog logItem = taskLogDao.getById(logId);
        if (logItem != null) {
            logItem.setEndTime(OffsetDateTime.now());
            logItem.setStatus(success ? "SUCCESS" : "FAILED");
            if (StrUtil.isNotBlank(content)) {
                String current = logItem.getContent() == null ? "" : logItem.getContent() + "\n";
                logItem.setContent(current + content);
            }
            if (StrUtil.isNotBlank(errorMsg)) {
                logItem.setErrorMsg(errorMsg);
            }
            if (logItem.getEndTime() != null && logItem.getStartTime() != null) {
                long ms = logItem.getEndTime().toInstant().toEpochMilli() - logItem.getStartTime().toInstant().toEpochMilli();
                logItem.setExecutionMs(ms);
            }
            taskLogDao.updateById(logItem);
        }
    }
}
