package cc.riskswap.trader.admin.stream;

import cc.riskswap.trader.base.dao.base.NodeDao;
import cc.riskswap.trader.base.dao.base.NodeMonitorDao;
import cc.riskswap.trader.base.dao.base.SystemTaskDao;
import cc.riskswap.trader.base.dao.base.TaskLogDao;
import cc.riskswap.trader.base.dao.base.entity.Node;
import cc.riskswap.trader.base.dao.base.entity.NodeMonitor;
import cc.riskswap.trader.base.dao.base.entity.SystemTask;
import cc.riskswap.trader.base.event.NodeMonitorEvent;
import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import cc.riskswap.trader.base.event.TaskLogEvent;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraderStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final TaskLogDao taskLogDao;
    private final NodeDao nodeDao;
    private final NodeMonitorDao nodeMonitorDao;
    private final SystemTaskDao systemTaskDao;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String eventType = message.getValue().get("eventType");
            String payload = message.getValue().get("payload");
            
            if (eventType == null || payload == null) {
                return;
            }

            switch (eventType) {
                case "TASK_LOG":
                    handleTaskLog(payload);
                    break;
                case "NODE_MONITOR":
                    handleNodeMonitor(payload);
                    break;
                case "SYSTEM_TASK_STATUS":
                    handleSystemTaskStatus(payload);
                    break;
                default:
                    log.warn("Unknown event type received from stream: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing stream message: {}", message.getId(), e);
        }
    }

    private void handleTaskLog(String payload) {
        TaskLogEvent event = JSONUtil.toBean(payload, TaskLogEvent.class);
        if ("RUNNING".equals(event.getStatus())) {
            taskLogDao.createRunningLog(event.getTaskName(), event.getTaskCode(), OffsetDateTime.now().toLocalDateTime(), event.getTraceId());
        } else if ("SUCCESS".equals(event.getStatus())) {
            taskLogDao.updateLogByTraceId(event.getTraceId(), "SUCCESS", event.getCostMs(), event.getRemark());
        } else if ("FAILED".equals(event.getStatus())) {
            taskLogDao.updateLogByTraceId(event.getTraceId(), "FAILED", event.getCostMs(), event.getRemark());
        }
    }

    private void handleNodeMonitor(String payload) {
        NodeMonitorEvent event = JSONUtil.toBean(payload, NodeMonitorEvent.class);
        
        Node node = nodeDao.getByNodeId(event.getNodeId());
        if (node == null) {
            node = new Node();
            node.setNodeId(event.getNodeId());
            node.setNodeType(event.getNodeType());
            node.setNodeName(event.getAppName());
            nodeDao.save(node);
        }

        NodeMonitor monitor = new NodeMonitor();
        monitor.setNodeId(event.getNodeId());
        monitor.setStatus(event.getStatus());
        monitor.setTimestamp(OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(event.getTimestamp()), ZoneOffset.UTC));
        nodeMonitorDao.save(monitor);
    }

    private void handleSystemTaskStatus(String payload) {
        SystemTaskStatusEvent event = JSONUtil.toBean(payload, SystemTaskStatusEvent.class);
        SystemTask task = systemTaskDao.getByTaskTypeAndTaskCode(event.getTaskType(), event.getTaskCode());
        if (task != null) {
            task.setStatus(event.getStatus());
            task.setResult(event.getResult());
            task.setVersion(event.getVersion());
            systemTaskDao.updateById(task);
        }
    }
}
