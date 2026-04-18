package cc.riskswap.trader.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.admin.dao.entity.Broker;

@MysqlMapper
public interface BrokerMapper extends BaseMapper<Broker> {
}
