package cc.riskswap.trader.executor.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.riskswap.trader.executor.dao.entity.InvestmentTrading;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;

@MysqlMapper
public interface InvestmentTradingMapper extends BaseMapper<InvestmentTrading> {
}