package cc.riskswap.trader.base.dao.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("node_monitor")
public class NodeMonitor {
    private String nodeId;
    private String status;
    private OffsetDateTime timestamp;
    private Float cpuUsage;
    private Float memoryUsage;
}
