package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.InvestmentTrading;
import cc.riskswap.trader.executor.dao.mapper.InvestmentTradingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Repository
public class InvestmentTradingDao extends ServiceImpl<InvestmentTradingMapper, InvestmentTrading> {

    public List<InvestmentTrading> getListByInvestmentId(Long investmentId) {
        LambdaQueryWrapper<InvestmentTrading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvestmentTrading::getInvestmentId, investmentId);
        return this.list(wrapper);
    }
}