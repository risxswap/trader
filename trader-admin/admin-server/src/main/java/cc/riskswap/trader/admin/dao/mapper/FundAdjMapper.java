package cc.riskswap.trader.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.admin.dao.entity.FundAdj;

@ClickHouseMapper
public interface FundAdjMapper extends BaseMapper<FundAdj> {
}
