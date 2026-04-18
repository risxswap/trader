package cc.riskswap.trader.base.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trader.task")
public class TraderTaskProperties {

    private boolean enabled = true;
    private long refreshPollMs = 60000;
    private long lockExpireSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getRefreshPollMs() {
        return refreshPollMs;
    }

    public void setRefreshPollMs(long refreshPollMs) {
        this.refreshPollMs = refreshPollMs;
    }

    public long getLockExpireSeconds() {
        return lockExpireSeconds;
    }

    public void setLockExpireSeconds(long lockExpireSeconds) {
        this.lockExpireSeconds = lockExpireSeconds;
    }
}
