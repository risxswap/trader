package cc.riskswap.trader.base.task;

public interface TraderTask {

    String getTaskCode();

    String getTaskName();

    boolean defaultEnabled();

    String getParamSchema();

    String getDefaultParams();

    void execute(TraderTaskContext context);
}
