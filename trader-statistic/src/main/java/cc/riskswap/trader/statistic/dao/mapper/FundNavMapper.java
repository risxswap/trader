package cc.riskswap.trader.statistic.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.statistic.dao.entity.FundNav;

@ClickHouseMapper
public interface FundNavMapper extends BaseMapper<FundNav> {
}
