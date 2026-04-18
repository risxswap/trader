package cc.riskswap.trader.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.admin.dao.entity.InvestmentPosition;

@MysqlMapper
public interface InvestmentPositionMapper extends BaseMapper<InvestmentPosition> {
}
