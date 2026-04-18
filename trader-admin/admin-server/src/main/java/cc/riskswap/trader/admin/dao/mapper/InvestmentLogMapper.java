package cc.riskswap.trader.admin.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.admin.dao.entity.InvestmentLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投资日志Mapper接口
 */
@Mapper
@MysqlMapper
public interface InvestmentLogMapper extends BaseMapper<InvestmentLog> {
}
