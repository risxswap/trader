package cc.riskswap.trader.base.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "trader.monitor")
public class TraderMonitorProperties {

    private boolean enabled;
    private Duration interval = Duration.ofSeconds(30);
    private boolean logOnPublish;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    public boolean isLogOnPublish() {
        return logOnPublish;
    }

    public void setLogOnPublish(boolean logOnPublish) {
        this.logOnPublish = logOnPublish;
    }
}
