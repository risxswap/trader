package cc.riskswap.trader.base.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.dao.entity.ImportLog;

@MysqlMapper
public interface ImportLogMapper extends BaseMapper<ImportLog>{

}
