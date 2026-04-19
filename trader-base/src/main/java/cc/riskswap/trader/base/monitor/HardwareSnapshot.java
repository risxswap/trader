package cc.riskswap.trader.base.monitor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HardwareSnapshot(
        String nodeId,
        String nodeType,
        String nodeName,
        OffsetDateTime collectedAt,
        String hostname,
        String primaryIp,
        BigDecimal cpuLoad,
        Long physicalMemoryTotal,
        Long physicalMemoryAvailable,
        Long diskTotal,
        Long diskAvailable,
        Long jvmUptime,
        Integer processCount,
        Integer threadCount
) {
}
