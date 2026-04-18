package cc.riskswap.trader.admin.dao.mapper;

import cc.riskswap.trader.admin.dao.entity.NodeMonitor;
import cc.riskswap.trader.base.datasource.annotation.ClickHouseMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@ClickHouseMapper
public interface NodeMonitorMapper extends BaseMapper<NodeMonitor> {
}
