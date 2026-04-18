package cc.riskswap.trader.admin.dao.base.mapper;

import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import cc.riskswap.trader.admin.dao.base.entity.TaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@MysqlMapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
}