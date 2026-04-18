package cc.riskswap.trader.collector.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.collector.repository.dao.mapper.FundNavMapper;
import cc.riskswap.trader.collector.repository.entity.FundNav;

import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class FundNavDao extends ServiceImpl<FundNavMapper, FundNav> {

    public void deleteByCode(String code) {
        LambdaQueryWrapper<FundNav> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundNav::getCode, code);
        baseMapper.delete(queryWrapper);
    }

    public void deleteByTime(OffsetDateTime time) {
        LambdaQueryWrapper<FundNav> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundNav::getTime, time);
        baseMapper.delete(queryWrapper);
    }

    public OffsetDateTime getLatestNavDate() {
        LambdaQueryWrapper<FundNav> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FundNav::getTime).last("limit 1");
        FundNav fundNav = baseMapper.selectOne(queryWrapper);
        return fundNav != null ? fundNav.getTime() : null;
    }
}
