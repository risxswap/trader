package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("node_monitor")
public class NodeMonitor {
    private String nodeId;
    private String nodeType;
    private String nodeName;
    @TableField("collected_at")
    private OffsetDateTime timestamp;
    private String hostname;
    private String primaryIp;
    private BigDecimal cpuLoad;
    private Long physicalMemoryTotal;
    private Long physicalMemoryAvailable;
    private Long diskTotal;
    private Long diskAvailable;
    private Long jvmUptime;
    private Integer processCount;
    private Integer threadCount;
    @TableField(exist = false)
    private Float cpuUsage;
    @TableField(exist = false)
    private Float memoryUsage;
}
