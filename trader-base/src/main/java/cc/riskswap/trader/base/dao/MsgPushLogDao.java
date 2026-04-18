package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.MsgPushLog;
import cc.riskswap.trader.base.dao.mapper.MsgPushLogMapper;
import cc.riskswap.trader.base.dao.query.MsgPushLogListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository("adminMsgPushLogDao")
public class MsgPushLogDao extends ServiceImpl<MsgPushLogMapper, MsgPushLog> {
    public Page<MsgPushLog> pageQuery(MsgPushLogListQuery query) {
        Page<MsgPushLog> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<MsgPushLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getType())) {
            wrapper.eq(MsgPushLog::getType, query.getType());
        }
        if (StrUtil.isNotBlank(query.getChannel())) {
            wrapper.eq(MsgPushLog::getChannel, query.getChannel());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(MsgPushLog::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(MsgPushLog::getCreatedAt);
        return this.page(page, wrapper);
    }
}
