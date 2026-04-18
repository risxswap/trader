package cc.riskswap.trader.executor.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.riskswap.trader.executor.dao.entity.Fund;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;

@MysqlMapper
public interface FundMapper extends BaseMapper<Fund> {
}