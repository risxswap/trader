package cc.riskswap.trader.collector.repository.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.collector.repository.entity.Fund;

@MysqlMapper
public interface FundMapper extends BaseMapper<Fund>{

}
