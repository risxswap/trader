package cc.riskswap.trader.base.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.base.dao.entity.FundMarket;
/**
 * 合约市场数据 Mapper 接口
 */
@ClickHouseMapper
public interface FundMarketMapper extends BaseMapper<FundMarket> {
}
