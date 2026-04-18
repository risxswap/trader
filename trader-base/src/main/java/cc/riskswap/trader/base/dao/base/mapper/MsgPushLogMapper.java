package cc.riskswap.trader.base.dao.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.dao.base.entity.MsgPushLog;

/**
 * 消息推送日志Mapper接口
 */
@MysqlMapper
public interface MsgPushLogMapper extends BaseMapper<MsgPushLog> {

}
