package cc.riskswap.trader.admin.dao.base;

import cc.riskswap.trader.admin.dao.base.entity.SystemTask;
import cc.riskswap.trader.admin.dao.base.mapper.SystemTaskMapper;
import cc.riskswap.trader.admin.dao.base.query.SystemTaskListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemTaskDao extends ServiceImpl<SystemTaskMapper, SystemTask> {

    public SystemTask getByTaskTypeAndTaskCode(String taskType, String taskCode) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getTaskType, taskType);
        wrapper.eq(SystemTask::getTaskCode, taskCode);
        return this.getOne(wrapper);
    }

    public List<SystemTask> listByTaskType(String taskType) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getTaskType, taskType);
        wrapper.orderByAsc(SystemTask::getTaskCode);
        return this.list(wrapper);
    }

    public SystemTask getByAppNameAndTaskCode(String appName, String taskCode) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getAppName, appName);
        wrapper.eq(SystemTask::getTaskCode, taskCode);
        return this.getOne(wrapper);
    }

    public List<SystemTask> listByAppName(String appName) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getAppName, appName);
        wrapper.orderByAsc(SystemTask::getTaskCode);
        return this.list(wrapper);
    }

    public Page<SystemTask> pageQuery(SystemTaskListQuery query) {
        Page<SystemTask> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTaskType())) {
            wrapper.eq(SystemTask::getTaskType, query.getTaskType());
        }
        if (StrUtil.isNotBlank(query.getAppName())) {
            wrapper.eq(SystemTask::getAppName, query.getAppName());
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
        wrapper.orderByAsc(SystemTask::getTaskType).orderByAsc(SystemTask::getTaskCode);
        return this.page(page, wrapper);
    }
}
