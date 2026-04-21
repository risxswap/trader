package cc.riskswap.trader.statistic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trader.statistic.correlation")
public class StatisticCorrelationProperties {

    private static final int DEFAULT_NAV_QUERY_CODE_BATCH_SIZE = 200;
    private static final int DEFAULT_SAVE_BATCH_SIZE = 200;
    private static final int DEFAULT_CLEANUP_DELETE_BATCH_SIZE = 200;

    private int navQueryCodeBatchSize = DEFAULT_NAV_QUERY_CODE_BATCH_SIZE;
    private int saveBatchSize = DEFAULT_SAVE_BATCH_SIZE;
    private int cleanupDeleteBatchSize = DEFAULT_CLEANUP_DELETE_BATCH_SIZE;

    public int getNavQueryCodeBatchSize() {
        return navQueryCodeBatchSize;
    }

    public void setNavQueryCodeBatchSize(int navQueryCodeBatchSize) {
        this.navQueryCodeBatchSize = navQueryCodeBatchSize;
    }

    public int getSafeNavQueryCodeBatchSize() {
        return navQueryCodeBatchSize > 0 ? navQueryCodeBatchSize : DEFAULT_NAV_QUERY_CODE_BATCH_SIZE;
    }

    public int getSaveBatchSize() {
        return saveBatchSize;
    }

    public void setSaveBatchSize(int saveBatchSize) {
        this.saveBatchSize = saveBatchSize;
    }

    public int getSafeSaveBatchSize() {
        return saveBatchSize > 0 ? saveBatchSize : DEFAULT_SAVE_BATCH_SIZE;
    }

    public int getCleanupDeleteBatchSize() {
        return cleanupDeleteBatchSize;
    }

    public void setCleanupDeleteBatchSize(int cleanupDeleteBatchSize) {
        this.cleanupDeleteBatchSize = cleanupDeleteBatchSize;
    }

    public int getSafeCleanupDeleteBatchSize() {
        return cleanupDeleteBatchSize > 0 ? cleanupDeleteBatchSize : DEFAULT_CLEANUP_DELETE_BATCH_SIZE;
    }
}
