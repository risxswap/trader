package cc.riskswap.trader.admin.dao.mapper;

import cc.riskswap.trader.admin.dao.entity.SystemTask;
import cc.riskswap.trader.base.datasource.annotation.MysqlMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@MysqlMapper
public interface SystemTaskMapper extends BaseMapper<SystemTask> {
}
