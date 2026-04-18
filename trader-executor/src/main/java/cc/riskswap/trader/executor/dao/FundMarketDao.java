package cc.riskswap.trader.executor.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.executor.dao.entity.FundMarket;
import cc.riskswap.trader.executor.dao.mapper.FundMarketMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;

@Repository
public class FundMarketDao extends ServiceImpl<FundMarketMapper, FundMarket> {

    public List<FundMarket> getDailyData(String fundCode, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<FundMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundMarket::getFundCode, fundCode);
        if (startDate != null) {
            wrapper.ge(FundMarket::getTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(FundMarket::getTime, endDate);
        }
        wrapper.orderByAsc(FundMarket::getTime);
        return this.list(wrapper);
    }
}