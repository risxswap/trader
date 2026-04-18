package cc.riskswap.trader.admin.dao;

import cc.riskswap.trader.admin.dao.entity.SystemTask;
import cc.riskswap.trader.admin.dao.mapper.SystemTaskMapper;
import cc.riskswap.trader.admin.dao.query.SystemTaskListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository("adminSystemTaskDao")
public class SystemTaskDao extends ServiceImpl<SystemTaskMapper, SystemTask> {

    public Page<SystemTask> pageQuery(SystemTaskListQuery query) {
        Page<SystemTask> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getAppName())) {
            wrapper.eq(SystemTask::getAppName, query.getAppName());
        }
        if (StrUtil.isNotBlank(query.getTaskType())) {
            wrapper.eq(SystemTask::getTaskType, query.getTaskType());
        }
        if (StrUtil.isNotBlank(query.getTaskCode())) {
            wrapper.like(SystemTask::getTaskCode, query.getTaskCode());
        }
        if (StrUtil.isNotBlank(query.getTaskName())) {
            wrapper.like(SystemTask::getTaskName, query.getTaskName());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SystemTask::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SystemTask::getCreatedAt);
        return this.page(page, wrapper);
    }

    public SystemTask getByAppNameAndTaskCode(String appName, String taskCode) {
        if (StrUtil.isBlank(appName) || StrUtil.isBlank(taskCode)) {
            return null;
        }
        return lambdaQuery()
                .eq(SystemTask::getTaskCode, taskCode)
                .and(w -> w.eq(SystemTask::getTaskType, appName).or().eq(SystemTask::getAppName, appName))
                .last("LIMIT 1")
                .one();
    }
}
