package cc.riskswap.trader.executor.pubsub;

public enum Channels {
    INVESTMENT("investment"),
    INVESTMENT_LOG("investment_log");

    public final String code;

    Channels(String code) {
        this.code = code;
    }
}
