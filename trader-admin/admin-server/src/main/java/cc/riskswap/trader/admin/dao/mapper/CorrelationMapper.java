package cc.riskswap.trader.admin.dao.mapper;

import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import cc.riskswap.trader.admin.dao.entity.Correlation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 证券相关性Mapper接口
 */
@ClickHouseMapper
@Mapper
public interface CorrelationMapper extends BaseMapper<Correlation> {
}
