package cc.riskswap.trader.admin.dao;

import java.util.List;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;

import cc.riskswap.trader.admin.dao.entity.InvestmentTrading;
import cc.riskswap.trader.admin.dao.mapper.InvestmentTradingMapper;
import cc.riskswap.trader.admin.dao.query.InvestmentTradingListQuery;

/**
 * 交易DAO类
 */
@Repository
public class InvestmentTradingDao extends ServiceImpl<InvestmentTradingMapper, InvestmentTrading> {

    public Page<InvestmentTrading> pageQuery(InvestmentTradingListQuery q) {
        Page<InvestmentTrading> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<InvestmentTrading> wrapper = new LambdaQueryWrapper<>();
        if (q.getInvestmentId() != null) {
            wrapper.eq(InvestmentTrading::getInvestmentId, q.getInvestmentId());
        }
        if (q.getInvestmentLogId() != null) {
            wrapper.eq(InvestmentTrading::getInvestmentLogId, q.getInvestmentLogId());
        }
        if (StrUtil.isNotBlank(q.getAsset())) {
            wrapper.eq(InvestmentTrading::getAsset, q.getAsset());
        }
        wrapper.orderByDesc(InvestmentTrading::getCreatedAt);
        return this.page(page, wrapper);
    }

    /**
     * Get the latest investment trading record by investment ID.
     *
     * @param investmentId Investment ID
     * @return The latest InvestmentTrading or null if not found
     */
    public InvestmentTrading getLatestByInvestmentId(Integer investmentId) {
        return lambdaQuery()
                .eq(InvestmentTrading::getInvestmentId, investmentId)
                .orderByDesc(InvestmentTrading::getCreatedAt)
                .last("LIMIT 1")
                .one();
    }

    public List<InvestmentTrading> getListByInvestmentId(Integer investmentId) {
        return lambdaQuery()
                .eq(InvestmentTrading::getInvestmentId, investmentId)
                .orderByDesc(InvestmentTrading::getCreatedAt)
                .list();
    }

    public List<InvestmentTrading> getListByInvestmentLogId(Integer investmentLogId) {
        return lambdaQuery()
                .eq(InvestmentTrading::getInvestmentLogId, investmentLogId)
                .list();
    }
}
