package cc.riskswap.trader.base.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.base.dao.entity.FundNav;

@ClickHouseMapper
public interface FundNavMapper extends BaseMapper<FundNav> {
}
