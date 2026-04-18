package cc.riskswap.trader.base.dao.base;

import cc.riskswap.trader.base.dao.base.entity.SystemTaskRunLog;
import cc.riskswap.trader.base.dao.base.mapper.SystemTaskRunLogMapper;
import cc.riskswap.trader.base.dao.base.query.SystemTaskRunLogListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class SystemTaskRunLogDao extends ServiceImpl<SystemTaskRunLogMapper, SystemTaskRunLog> {

    public Page<SystemTaskRunLog> pageQuery(SystemTaskRunLogListQuery query) {
        Page<SystemTaskRunLog> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SystemTaskRunLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getAppName())) {
            wrapper.eq(SystemTaskRunLog::getAppName, query.getAppName());
        }
        if (StrUtil.isNotBlank(query.getTaskCode())) {
            wrapper.like(SystemTaskRunLog::getTaskCode, query.getTaskCode());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SystemTaskRunLog::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getTriggerType())) {
            wrapper.eq(SystemTaskRunLog::getTriggerType, query.getTriggerType());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SystemTaskRunLog::getStartedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SystemTaskRunLog::getStartedAt, query.getEndTime());
        }
        wrapper.orderByDesc(SystemTaskRunLog::getCreatedAt);
        return this.page(page, wrapper);
    }
}
