package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.InvestmentPosition;
import cc.riskswap.trader.executor.dao.mapper.InvestmentPositionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Repository
public class InvestmentPositionDao extends ServiceImpl<InvestmentPositionMapper, InvestmentPosition> {

    public List<InvestmentPosition> getListByInvestmentId(Long investmentId) {
        LambdaQueryWrapper<InvestmentPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvestmentPosition::getInvestmentId, investmentId);
        return this.list(wrapper);
    }

    public InvestmentPosition getLatestByInvestmentIdAndAssetAndAssetType(Long investmentId, String asset, String assetType) {
        LambdaQueryWrapper<InvestmentPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvestmentPosition::getInvestmentId, investmentId)
               .eq(InvestmentPosition::getAsset, asset)
               .eq(InvestmentPosition::getAssetType, assetType)
               .orderByDesc(InvestmentPosition::getCreatedAt)
               .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }
}