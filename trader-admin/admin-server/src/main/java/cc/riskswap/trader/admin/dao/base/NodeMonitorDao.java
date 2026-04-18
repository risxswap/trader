package cc.riskswap.trader.admin.dao.base;

import cc.riskswap.trader.admin.dao.base.entity.NodeMonitor;
import cc.riskswap.trader.admin.dao.base.mapper.NodeMonitorMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class NodeMonitorDao extends ServiceImpl<NodeMonitorMapper, NodeMonitor> {

    public List<NodeMonitor> listHistory(String nodeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return baseMapper.listHistory(nodeId, startTime, endTime);
    }
}
