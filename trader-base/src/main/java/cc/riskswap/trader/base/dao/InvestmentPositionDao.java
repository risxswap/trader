package cc.riskswap.trader.base.dao;

import java.util.List;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;

import cc.riskswap.trader.base.dao.entity.InvestmentPosition;
import cc.riskswap.trader.base.dao.mapper.InvestmentPositionMapper;
import cc.riskswap.trader.base.dao.query.InvestmentPositionListQuery;

/**
 * 持仓DAO类
 */
@Repository
public class InvestmentPositionDao extends ServiceImpl<InvestmentPositionMapper, InvestmentPosition> {

    public Page<InvestmentPosition> pageQuery(InvestmentPositionListQuery q) {
        Page<InvestmentPosition> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<InvestmentPosition> wrapper = new LambdaQueryWrapper<>();
        if (q.getInvestmentId() != null) {
            wrapper.eq(InvestmentPosition::getInvestmentId, q.getInvestmentId());
        }
        if (q.getInvestmentLogId() != null) {
            wrapper.eq(InvestmentPosition::getInvestmentLogId, q.getInvestmentLogId());
        }
        if (StrUtil.isNotBlank(q.getAsset())) {
            wrapper.eq(InvestmentPosition::getAsset, q.getAsset());
        }
        wrapper.orderByDesc(InvestmentPosition::getCreatedAt);
        return this.page(page, wrapper);
    }

    /**
     * Get the latest investment position by investment ID.
     *
     * @param investmentId Investment ID
     * @return The latest InvestmentPosition or null if not found
     */
    public InvestmentPosition getLatestByInvestmentId(Integer investmentId) {
        return lambdaQuery()
                .eq(InvestmentPosition::getInvestmentId, investmentId)
                .orderByDesc(InvestmentPosition::getUpdatedAt)
                .last("LIMIT 1")
                .one();
    }

    public List<InvestmentPosition> getListByInvestmentId(Integer investmentId) {
        return lambdaQuery()
                .eq(InvestmentPosition::getInvestmentId, investmentId)
                .orderByDesc(InvestmentPosition::getUpdatedAt)
                .list();
    }

    public List<InvestmentPosition> getListByInvestmentLogId(Integer investmentLogId) {
        return lambdaQuery()
                .eq(InvestmentPosition::getInvestmentLogId, investmentLogId)
                .list();
    }

    public InvestmentPosition getLatestByInvestmentIdAndAssetAndAssetType(Integer investmentId, String asset, String assetType) {
        return lambdaQuery()
                .eq(InvestmentPosition::getInvestmentId, investmentId)
                .eq(InvestmentPosition::getAsset, asset)
                .eq(InvestmentPosition::getAssetType, assetType)
                .orderByDesc(InvestmentPosition::getUpdatedAt)
                .last("LIMIT 1")
                .one();
    }
}
