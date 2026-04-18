package cc.riskswap.trader.statistic.dao;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.statistic.dao.entity.FundNav;
import cc.riskswap.trader.statistic.dao.mapper.FundNavMapper;

@Repository
public class FundNavDao extends ServiceImpl<FundNavMapper, FundNav> {

    public List<FundNav> listByCodeAndStartTime(String code, OffsetDateTime startTime) {
        LambdaQueryWrapper<FundNav> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNav::getCode, code);
        wrapper.ge(FundNav::getTime, startTime);
        wrapper.orderByAsc(FundNav::getTime);
        return this.list(wrapper);
    }
}
