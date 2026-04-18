package cc.riskswap.trader.collector.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

import cc.riskswap.trader.collector.common.enums.ExchangeEnum;
import cc.riskswap.trader.collector.common.model.query.CalendarQuery;
import cc.riskswap.trader.collector.common.util.TaskContentContext;
import cc.riskswap.trader.collector.repository.dao.CalendarDao;
import cc.riskswap.trader.collector.repository.entity.Calendar;
import cc.riskswap.trader.collector.repository.tushare.CalendarTushare;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CalendarService {

    @Autowired
    private CalendarDao calendarDao;

    @Autowired
    private CalendarTushare calendarTushare;

    public void syncCalendar() {
        TaskContentContext.addAttribute("交易所数量", String.valueOf(ExchangeEnum.values().length));
        for (ExchangeEnum exchange : ExchangeEnum.values()) {
            syncByExchange(exchange);
        }
    }
    
    public void syncByExchange(ExchangeEnum exchange) {
        Calendar latest = calendarDao.getLatestByExchange(exchange.code);
        CalendarQuery query = new CalendarQuery();
        query.setExchange(exchange.code);
        if (latest != null) {
            LocalDate latestDate = latest.getDate();
            LocalDate firstDayOfYear = latestDate.withDayOfYear(1);
            LocalDate lastDayOfYear = latestDate.plusYears(1).withDayOfYear(latestDate.lengthOfYear());
            query.setStartDate(firstDayOfYear);
            query.setEndDate(lastDayOfYear);
        }
        List<Calendar> calendars = calendarTushare.list(query);
        if (CollectionUtils.isEmpty(calendars)) {
            TaskContentContext.addMetric("交易所空数据数", 1);
            TaskContentContext.addDetail("交易日历同步",
                    String.format("%s start=%s,end=%s 无数据", exchange.code, query.getStartDate(), query.getEndDate()));
            log.info("No calendar found for exchange: {}, startDate: {}, endDate: {}", exchange.code, query.getStartDate(), query.getEndDate());
            return;
        }
        log.info("Sync calendar for exchange: {}, startDate: {}, endDate: {}, total:{}", exchange.code, query.getStartDate(), query.getEndDate(), calendars.size());
        calendarDao.delete(exchange.code, query.getStartDate(), query.getEndDate());
        List<List<Calendar>> partitions = Lists.partition(calendars, 1000);
        for (List<Calendar> partition : partitions) {
            calendarDao.saveBatch(partition);
        }
        TaskContentContext.addMetric("交易所同步数", 1);
        TaskContentContext.addMetric("交易日历记录数", calendars.size());
        TaskContentContext.addMetric("交易日历批次数", partitions.size());
        TaskContentContext.addDetail("交易日历同步",
                String.format("%s start=%s,end=%s,记录=%d,批次=%d",
                        exchange.code, query.getStartDate(), query.getEndDate(), calendars.size(), partitions.size()));
        log.info("Sync calendar for exchange: {} done", exchange.code);
    }
}
