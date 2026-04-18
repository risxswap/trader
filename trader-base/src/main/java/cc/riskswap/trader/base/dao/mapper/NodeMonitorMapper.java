package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.dao.entity.NodeMonitor;
import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@ClickHouseMapper
public interface NodeMonitorMapper extends BaseMapper<NodeMonitor> {
}
