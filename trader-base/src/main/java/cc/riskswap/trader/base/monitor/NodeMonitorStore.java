package cc.riskswap.trader.base.monitor;

import cc.riskswap.trader.base.dao.NodeMonitorDao;
import cc.riskswap.trader.base.dao.entity.NodeMonitor;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

public class NodeMonitorStore {

    private static final String NODE_MONITOR_KEY = "node:monitor";

    private final NodeMonitorDao nodeMonitorDao;
    private final StringRedisTemplate stringRedisTemplate;

    public NodeMonitorStore(NodeMonitorDao nodeMonitorDao, StringRedisTemplate stringRedisTemplate) {
        this.nodeMonitorDao = nodeMonitorDao;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void write(HardwareSnapshot snapshot) {
        NodeMonitor monitor = new NodeMonitor();
        monitor.setNodeId(snapshot.nodeId());
        monitor.setNodeType(snapshot.nodeType());
        monitor.setNodeName(snapshot.nodeName());
        monitor.setTimestamp(snapshot.collectedAt());
        monitor.setHostname(snapshot.hostname());
        monitor.setPrimaryIp(snapshot.primaryIp());
        monitor.setCpuLoad(snapshot.cpuLoad());
        monitor.setPhysicalMemoryTotal(snapshot.physicalMemoryTotal());
        monitor.setPhysicalMemoryAvailable(snapshot.physicalMemoryAvailable());
        monitor.setDiskTotal(snapshot.diskTotal());
        monitor.setDiskAvailable(snapshot.diskAvailable());
        monitor.setJvmUptime(snapshot.jvmUptime());
        monitor.setProcessCount(snapshot.processCount());
        monitor.setThreadCount(snapshot.threadCount());
        nodeMonitorDao.save(monitor);
        stringRedisTemplate.opsForHash().put(NODE_MONITOR_KEY, snapshot.nodeId(), JSONUtil.toJsonStr(snapshot));
    }
}
