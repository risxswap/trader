package cc.riskswap.trader.collector.repository.dao;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.collector.repository.dao.mapper.FundMarketMapper;
import cc.riskswap.trader.collector.repository.entity.FundMarket;
import cn.hutool.core.collection.CollectionUtil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 市场数据DAO类
 */
@Repository
public class FundMarketDao extends ServiceImpl<FundMarketMapper, FundMarket> {
    
    /**
     * 获取最新的交易日期
     * @return 最新交易日期
     */
    public OffsetDateTime getLatestTradeDate() {
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FundMarket::getTime);
        queryWrapper.last("LIMIT 1");
        FundMarket fundMarket = baseMapper.selectOne(queryWrapper);
        return fundMarket != null ? fundMarket.getTime() : null;
    }

    /**
     * 根据基金代码获取日级行情数据
     * @param tsCode 基金TS代码
     * @param start 开始日期
     * @param end 结束日期
     * @return 日级数据结果集
     */
    public List<FundMarket> getDailyData(String tsCode, LocalDate start, LocalDate end) {
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startDateTime = start.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endDateTime = end.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundMarket::getCode, tsCode);
        queryWrapper.ge(FundMarket::getTime, startDateTime);
        queryWrapper.lt(FundMarket::getTime, endDateTime);
        queryWrapper.orderByAsc(FundMarket::getTime);
        
        return baseMapper.selectList(queryWrapper);
    }
    
    /**
     * 根据基金代码和交易日期获取行情数据
     * @param tsCode 基金TS代码
     * @param tradeDate 交易日期 (格式: yyyyMMdd)
     * @return 行情数据
     */
    public FundMarket getByTsCodeAndTradeDate(String tsCode, String tradeDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(tradeDate, formatter);
        OffsetDateTime tradeDateStart = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime tradeDateEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundMarket::getCode, tsCode);
        queryWrapper.ge(FundMarket::getTime, tradeDateStart);
        queryWrapper.lt(FundMarket::getTime, tradeDateEnd);
        
        return baseMapper.selectOne(queryWrapper);
    }
    

    /**
     * Get fund market data by time and code
     * @param time Trade time
     * @param code Fund code
     * @return Fund market data
     */
    public FundMarket getByTimeAndCode(OffsetDateTime time, String code) {
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundMarket::getTime, time);
        queryWrapper.eq(FundMarket::getCode, code);
        
        return baseMapper.selectOne(queryWrapper);
    }
    
    /**
     * 根据基金代码删除行情数据
     * @param tsCode 基金TS代码
     * @return 删除记录数
     */
    public int deleteByCode(String tsCode, String timeFrame) {
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundMarket::getCode, tsCode);
        queryWrapper.eq(FundMarket::getTimeFrame, timeFrame);
        
        return baseMapper.delete(queryWrapper);
    }

    /**
     * Delete fund market data by specific time
     * @param time Trade time to delete
     * @return Number of deleted records
     */
    public int deleteByTime(OffsetDateTime time, String timeFrame) {
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundMarket::getTime, time);
        queryWrapper.eq(FundMarket::getTimeFrame, timeFrame);
        
        return baseMapper.delete(queryWrapper);
    }

    /**
     * 根据基金代码和日期范围删除行情数据
     * @param tsCode 基金TS代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 删除记录数
     */
    public int deleteByTsCodeAndDateRange(String tsCode, LocalDate startDate, LocalDate endDate) {
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startDateTime = startDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endDateTime = endDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundMarket::getCode, tsCode);
        queryWrapper.ge(FundMarket::getTime, startDateTime);
        queryWrapper.lt(FundMarket::getTime, endDateTime);
        
        return baseMapper.delete(queryWrapper);
    }

    public List<FundMarket> listByCodeAndDate(Set<String> symbols, LocalDate startDate, LocalDate endDate) {
        if (CollectionUtil.isEmpty(symbols)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<FundMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FundMarket::getCode, symbols);
        queryWrapper.ge(FundMarket::getTime, startDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        queryWrapper.le(FundMarket::getTime, endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        return baseMapper.selectList(queryWrapper);
    }
}
