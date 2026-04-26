package cc.riskswap.trader.base.monitor;

import cc.riskswap.trader.base.config.TraderNodeProperties;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

public class HardwareMonitorService {
    private static final Path MEMINFO_PATH = Path.of("/proc/meminfo");

    private final TraderNodeProperties nodeProperties;

    public HardwareMonitorService(TraderNodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    public HardwareSnapshot currentSnapshot() {
        String nodeId = nodeProperties.getId();
        if (!StringUtils.hasText(nodeId)) {
            return null;
        }

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        long diskTotal = 0L;
        long diskAvailable = 0L;
        for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
            try {
                diskTotal += fileStore.getTotalSpace();
                diskAvailable += fileStore.getUsableSpace();
            } catch (IOException ignored) {
                // Skip file stores we cannot inspect.
            }
        }

        return new HardwareSnapshot(
                nodeId,
                nodeProperties.getType(),
                nodeProperties.getName(),
                OffsetDateTime.now(),
                resolveHostname(),
                resolvePrimaryIp(),
                toCpuLoad(osBean.getCpuLoad()),
                osBean.getTotalMemorySize(),
                resolveAvailableMemory(osBean, MEMINFO_PATH),
                diskTotal,
                diskAvailable,
                runtimeMXBean.getUptime(),
                Math.toIntExact(Math.min(Integer.MAX_VALUE, ProcessHandle.allProcesses().count())),
                threadMXBean.getThreadCount()
        );
    }

    long resolveAvailableMemory(OperatingSystemMXBean osBean, Path memInfoPath) {
        Long memAvailable = readMemAvailable(memInfoPath);
        return memAvailable != null ? memAvailable : osBean.getFreeMemorySize();
    }

    Long readMemAvailable(Path memInfoPath) {
        if (memInfoPath == null || !Files.isReadable(memInfoPath)) {
            return null;
        }
        try {
            return parseMemAvailable(Files.readAllLines(memInfoPath));
        } catch (IOException ignored) {
            return null;
        }
    }

    Long parseMemAvailable(List<String> lines) {
        if (lines == null) {
            return null;
        }
        for (String line : lines) {
            if (line == null || !line.startsWith("MemAvailable:")) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2) {
                return null;
            }
            try {
                return Long.parseLong(parts[1]) * 1024L;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal toCpuLoad(double cpuLoad) {
        if (cpuLoad < 0) {
            return BigDecimal.ZERO;
        }
        if (cpuLoad > 1) {
            cpuLoad = 1;
        }
        return BigDecimal.valueOf(cpuLoad).setScale(4, RoundingMode.HALF_UP);
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return nodeProperties.getName();
        }
    }

    private String resolvePrimaryIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "";
        }
    }
}
