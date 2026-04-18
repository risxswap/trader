package cc.riskswap.trader.collector.repository.dao;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.collector.repository.dao.mapper.CalendarMapper;
import cc.riskswap.trader.collector.repository.entity.Calendar;

@Repository
public class CalendarDao extends ServiceImpl<CalendarMapper, Calendar> {

    /**
     * 查询日历列表
     * @param year
     * @return
     */
    public List<Calendar> list(Integer year) {
        return null;
    }

    /**
     * 根据exchange获取最新日历
     */
    public Calendar getLatestByExchange(String exchange) {
        LambdaQueryWrapper<Calendar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Calendar::getExchange, exchange)
                .orderByDesc(Calendar::getDate);
        queryWrapper.last("limit 1");
        return getOne(queryWrapper);
    }

    /**
     * 删除数据
     * @param exchange
     * @param beginDate
     * @param endDate
     */
    public void delete(String exchange, LocalDate beginDate, LocalDate endDate) {
        LambdaQueryWrapper<Calendar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Calendar::getExchange, exchange);
        if (beginDate != null) {
            queryWrapper.ge(Calendar::getDate, beginDate);
        }
        if (endDate != null) {
            queryWrapper.le(Calendar::getDate, endDate);
        }
        remove(queryWrapper);
    }
}