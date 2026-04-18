package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.NodeMonitor;
import cc.riskswap.trader.base.dao.mapper.NodeMonitorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository("adminNodeMonitorDao")
public class NodeMonitorDao extends ServiceImpl<NodeMonitorMapper, NodeMonitor> {
    public List<NodeMonitor> listHistory(String nodeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        LambdaQueryWrapper<NodeMonitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NodeMonitor::getNodeId, nodeId);
        if (startTime != null) {
            wrapper.ge(NodeMonitor::getTimestamp, startTime);
        }
        if (endTime != null) {
            wrapper.le(NodeMonitor::getTimestamp, endTime);
        }
        wrapper.orderByAsc(NodeMonitor::getTimestamp);
        List<NodeMonitor> logs = this.list(wrapper);
        logs.forEach(item -> {
            float cpu = item.getCpuLoad() == null ? 0f : item.getCpuLoad().floatValue();
            item.setCpuUsage(cpu < 0f ? 0f : Math.min(cpu, 1f));
            Long total = item.getPhysicalMemoryTotal();
            Long available = item.getPhysicalMemoryAvailable();
            if (total == null || total <= 0 || available == null) {
                item.setMemoryUsage(0f);
            } else {
                float usage = (float) (total - available) / total;
                item.setMemoryUsage(usage < 0f ? 0f : Math.min(usage, 1f));
            }
        });
        return logs;
    }
}
