package cc.riskswap.trader.executor.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.riskswap.trader.executor.dao.entity.FundMarket;
import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;

@ClickHouseMapper
public interface FundMarketMapper extends BaseMapper<FundMarket> {
}