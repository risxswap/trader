package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.TaskLog;
import cc.riskswap.trader.base.dao.mapper.TaskLogMapper;
import cc.riskswap.trader.base.dao.query.TaskLogListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository("adminTaskLogDao")
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
}
