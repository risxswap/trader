package cc.riskswap.trader.collector.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.collector.repository.dao.mapper.FundAdjMapper;
import cc.riskswap.trader.collector.repository.entity.FundAdj;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 基金调整因子数据访问层
 */
@Repository
public class FundAdjDao extends ServiceImpl<FundAdjMapper, FundAdj> {

    public OffsetDateTime getLatestTradeDate() {
        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FundAdj::getTime);
        queryWrapper.last("LIMIT 1");
        FundAdj fundAdj = baseMapper.selectOne(queryWrapper);
        return fundAdj != null ? fundAdj.getTime() : null;
    }

    public int deleteByCode(String tsCode) {
        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundAdj::getCode, tsCode);
        return baseMapper.delete(queryWrapper);
    }

    public int deleteByTime(OffsetDateTime time) {
        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundAdj::getTime, time);
        return baseMapper.delete(queryWrapper);
    }

    /**
     * 根据基金代码与日期范围获取复权因子，按时间升序
     */
    public List<FundAdj> listByCodeAndDateRange(String tsCode, LocalDate start, LocalDate end) {
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startDateTime = start.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endDateTime = end.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundAdj::getCode, tsCode);
        queryWrapper.ge(FundAdj::getTime, startDateTime);
        queryWrapper.lt(FundAdj::getTime, endDateTime);
        queryWrapper.orderByAsc(FundAdj::getTime);
        return baseMapper.selectList(queryWrapper);
    }
}
