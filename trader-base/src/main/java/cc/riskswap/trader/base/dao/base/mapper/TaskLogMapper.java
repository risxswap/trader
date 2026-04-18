package cc.riskswap.trader.base.dao.base.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.base.dao.base.entity.TaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
}