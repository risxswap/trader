package cc.riskswap.trader.collector.repository.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.collector.repository.entity.FundNav;

@ClickHouseMapper
public interface FundNavMapper extends BaseMapper<FundNav> {
}
