package cc.riskswap.trader.base.monitor;

import cc.riskswap.trader.base.config.TraderNodeProperties;
import com.sun.management.OperatingSystemMXBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

class HardwareMonitorServiceTest {

    @Test
    void shouldParseMemAvailableFromProcMeminfo() {
        HardwareMonitorService service = new HardwareMonitorService(nodeProperties());

        Long result = service.parseMemAvailable(List.of(
                "MemTotal:       16321952 kB",
                "MemFree:          855040 kB",
                "MemAvailable:   10564832 kB",
                "Buffers:          125120 kB"
        ));

        Assertions.assertEquals(10564832L * 1024L, result);
    }

    @Test
    void shouldFallbackToFreeMemoryWhenProcMeminfoUnreadable() {
        HardwareMonitorService service = new HardwareMonitorService(nodeProperties());
        OperatingSystemMXBean osBean = Mockito.mock(OperatingSystemMXBean.class);
        Mockito.when(osBean.getFreeMemorySize()).thenReturn(835L * 1024 * 1024);

        long result = service.resolveAvailableMemory(osBean, Path.of("/tmp/not-used"));

        Assertions.assertEquals(835L * 1024 * 1024, result);
    }

    @Test
    void shouldFallbackToFreeMemoryWhenMemAvailableMissing() {
        HardwareMonitorService service = Mockito.spy(new HardwareMonitorService(nodeProperties()));
        OperatingSystemMXBean osBean = Mockito.mock(OperatingSystemMXBean.class);
        Mockito.when(osBean.getFreeMemorySize()).thenReturn(835L * 1024 * 1024);
        Mockito.doReturn(null).when(service).readMemAvailable(Path.of("/tmp/meminfo"));

        long result = service.resolveAvailableMemory(osBean, Path.of("/tmp/meminfo"));

        Assertions.assertEquals(835L * 1024 * 1024, result);
    }

    @Test
    void shouldUseMemAvailableWhenPresent() {
        HardwareMonitorService service = Mockito.spy(new HardwareMonitorService(nodeProperties()));
        OperatingSystemMXBean osBean = Mockito.mock(OperatingSystemMXBean.class);
        Mockito.when(osBean.getFreeMemorySize()).thenReturn(835L * 1024 * 1024);
        Mockito.doReturn(10L * 1024 * 1024 * 1024).when(service).readMemAvailable(Path.of("/tmp/meminfo"));

        long result = service.resolveAvailableMemory(osBean, Path.of("/tmp/meminfo"));

        Assertions.assertEquals(10L * 1024 * 1024 * 1024, result);
    }

    private TraderNodeProperties nodeProperties() {
        TraderNodeProperties properties = new TraderNodeProperties();
        properties.setId("node-1");
        properties.setType("executor");
        properties.setName("执行器");
        return properties;
    }
}
