package cc.riskswap.trader.base.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.dao.entity.InvestmentTrading;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易Mapper接口
 */
@Mapper
@MysqlMapper
public interface InvestmentTradingMapper extends BaseMapper<InvestmentTrading> {

}
