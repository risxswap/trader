package cc.riskswap.trader.base.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class HardwareMonitorPublisher {

    private final HardwareMonitorService hardwareMonitorService;
    private final NodeMonitorStore nodeMonitorStore;
    private final TraderMonitorProperties traderMonitorProperties;

    public HardwareMonitorPublisher(
            HardwareMonitorService hardwareMonitorService,
            NodeMonitorStore nodeMonitorStore,
            TraderMonitorProperties traderMonitorProperties
    ) {
        this.hardwareMonitorService = hardwareMonitorService;
        this.nodeMonitorStore = nodeMonitorStore;
        this.traderMonitorProperties = traderMonitorProperties;
    }

    @Scheduled(fixedDelayString = "#{@traderMonitorProperties.getInterval().toMillis()}")
    public void publish() {
        HardwareSnapshot snapshot = hardwareMonitorService.currentSnapshot();
        if (snapshot == null) {
            return;
        }
        nodeMonitorStore.write(snapshot);
        if (traderMonitorProperties.isLogOnPublish()) {
            log.info("Published hardware snapshot nodeId={} nodeType={}", snapshot.nodeId(), snapshot.nodeType());
        }
    }
}
