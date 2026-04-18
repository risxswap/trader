package cc.riskswap.trader.base.task;

import org.springframework.scheduling.annotation.Scheduled;

public class TraderTaskPollingJob {

    private final TraderTaskPoller traderTaskPoller;

    public TraderTaskPollingJob(TraderTaskPoller traderTaskPoller) {
        this.traderTaskPoller = traderTaskPoller;
    }

    @Scheduled(fixedDelayString = "${trader.task.refresh-poll-ms:60000}")
    public void poll() throws Exception {
        traderTaskPoller.poll();
    }
}
