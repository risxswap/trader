package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.InvestmentLog;
import cc.riskswap.trader.executor.dao.mapper.InvestmentLogMapper;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Repository
public class InvestmentLogDao extends ServiceImpl<InvestmentLogMapper, InvestmentLog> {

    public InvestmentLog getLatestByInvestmentId(Long investmentId) {
        LambdaQueryWrapper<InvestmentLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvestmentLog::getInvestmentId, investmentId)
               .orderByDesc(InvestmentLog::getRecordDate)
               .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }
}