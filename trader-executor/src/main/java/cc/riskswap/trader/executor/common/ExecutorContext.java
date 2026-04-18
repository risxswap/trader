package cc.riskswap.trader.executor.common;

import cc.riskswap.trader.executor.dao.entity.Investment;
import cc.riskswap.trader.executor.dao.entity.InvestmentLog;
import cc.riskswap.trader.executor.dao.entity.InvestmentPosition;
import cc.riskswap.trader.executor.dao.entity.InvestmentTrading;

import java.util.HashMap;
import java.util.Map;

public class ExecutorContext {
    private Investment investment;
    private InvestmentLog latestLog;
    private Map<String, InvestmentPosition> positions = new HashMap<>();
    private Map<String, InvestmentTrading> traddings = new HashMap<>();

    public Investment getInvestment() {
        return investment;
    }

    public void setInvestment(Investment investment) {
        this.investment = investment;
    }

    public InvestmentLog getLatestLog() {
        return latestLog;
    }

    public void setLatestLog(InvestmentLog latestLog) {
        this.latestLog = latestLog;
    }

    public Map<String, InvestmentPosition> getPositions() {
        return positions;
    }

    public void setPositions(Map<String, InvestmentPosition> positions) {
        this.positions = positions;
    }

    public Map<String, InvestmentTrading> getTraddings() {
        return traddings;
    }

    public void setTraddings(Map<String, InvestmentTrading> traddings) {
        this.traddings = traddings;
    }

    public String getAssetKey(String asset, String assetType) {
        return asset + "-" + assetType;
    }
}
