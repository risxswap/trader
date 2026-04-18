package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.dao.entity.Investment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投资Mapper接口
 */
@Mapper
@MysqlMapper
public interface InvestmentMapper extends BaseMapper<Investment> {
}
