package cc.riskswap.trader.base.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.base.dao.entity.FundAdj;
import cc.riskswap.trader.base.dao.mapper.FundAdjMapper;
import cc.riskswap.trader.base.dao.query.FundAdjListQuery;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
public class FundAdjDao extends ServiceImpl<FundAdjMapper, FundAdj> {

    public OffsetDateTime getLatestTradeDate() {
        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FundAdj::getTime);
        queryWrapper.last("LIMIT 1");
        FundAdj fundAdj = baseMapper.selectOne(queryWrapper);
        return fundAdj != null ? fundAdj.getTime() : null;
    }

    public int deleteByCode(String code) {
        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundAdj::getCode, code);
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
    public List<FundAdj> listByCodeAndDateRange(String code, LocalDate start, LocalDate end) {
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startDateTime = start.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endDateTime = end.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        
        LambdaQueryWrapper<FundAdj> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundAdj::getCode, code);
        queryWrapper.ge(FundAdj::getTime, startDateTime);
        queryWrapper.lt(FundAdj::getTime, endDateTime);
        queryWrapper.orderByAsc(FundAdj::getTime);
        return baseMapper.selectList(queryWrapper);
    }

    public Page<FundAdj> pageQuery(FundAdjListQuery q) {
        Page<FundAdj> page = new Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<FundAdj> wrapper = new LambdaQueryWrapper<>();
        if (q.getCode() != null && !q.getCode().isEmpty()) {
            wrapper.eq(FundAdj::getCode, q.getCode());
        }
        if (q.getStartDate() != null) {
            wrapper.ge(FundAdj::getTime, q.getStartDate().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (q.getEndDate() != null) {
            wrapper.lt(FundAdj::getTime, q.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        wrapper.orderByDesc(FundAdj::getTime);
        return this.page(page, wrapper);
    }
}
