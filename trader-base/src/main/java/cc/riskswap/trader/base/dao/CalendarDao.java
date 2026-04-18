package cc.riskswap.trader.base.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.riskswap.trader.base.dao.entity.Calendar;
import cc.riskswap.trader.base.dao.mapper.CalendarMapper;
import cc.riskswap.trader.base.dao.query.CalendarListQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import cn.hutool.core.util.StrUtil;

@Repository
public class CalendarDao extends ServiceImpl<CalendarMapper, Calendar> {

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Calendar> pageQuery(CalendarListQuery q) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Calendar> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(q.getPageNo(), q.getPageSize());
        LambdaQueryWrapper<Calendar> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q.getExchange())) {
            wrapper.eq(Calendar::getExchange, q.getExchange());
        }
        if (q.getStartDate() != null) {
            wrapper.ge(Calendar::getDate, q.getStartDate());
        }
        if (q.getEndDate() != null) {
            wrapper.le(Calendar::getDate, q.getEndDate());
        }
        wrapper.orderByDesc(Calendar::getDate);
        return this.page(page, wrapper);
    }

    public Calendar getLatestByExchange(String exchange) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Calendar> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Calendar::getExchange, exchange);
        wrapper.orderByDesc(Calendar::getDate);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }

    public void delete(String exchange, LocalDate startDate, LocalDate endDate) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Calendar> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Calendar::getExchange, exchange);
        if (startDate != null) {
            wrapper.ge(Calendar::getDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(Calendar::getDate, endDate);
        }
        this.remove(wrapper);
    }/**
     * 查询日历列表
     * @param year
     * @return
     */
    public List<Calendar> list(Integer year) {
        return null;
    }

    /**
     * 根据日期查询
     * @param beginDate
     * @param endDate
     * @return
     */
    public List<Calendar> listByDate(LocalDate beginDate, LocalDate endDate) {
        LambdaQueryWrapper<Calendar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(Calendar::getDate, beginDate, endDate);
        queryWrapper.orderByAsc(Calendar::getDate);
        return list(queryWrapper);
    }
}