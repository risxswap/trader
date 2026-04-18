package cc.riskswap.trader.base.dao.base;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;

import cc.riskswap.trader.base.dao.base.entity.MsgPushLog;
import cc.riskswap.trader.base.dao.base.mapper.MsgPushLogMapper;
import cc.riskswap.trader.base.dao.base.query.MsgPushLogListQuery;

/**
 * 消息推送日志DAO类
 */
@Repository
public class MsgPushLogDao extends ServiceImpl<MsgPushLogMapper, MsgPushLog> {

    public Page<MsgPushLog> pageQuery(MsgPushLogListQuery query) {
        Page<MsgPushLog> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<MsgPushLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(MsgPushLog::getType, query.getType());
        }
        if (StringUtils.hasText(query.getChannel())) {
            wrapper.eq(MsgPushLog::getChannel, query.getChannel());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MsgPushLog::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(MsgPushLog::getCreatedAt);

        return this.page(page, wrapper);
    }
}
