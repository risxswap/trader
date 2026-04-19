package cc.riskswap.trader.collector.test.repository.tushare;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.alibaba.fastjson2.JSON;

import cc.riskswap.trader.collector.common.model.query.CalendarQuery;
import cc.riskswap.trader.collector.repository.tushare.CalendarTushare;
import cc.riskswap.trader.base.dao.entity.Calendar;

import java.time.LocalDate;
import java.util.List;

/**
 * Unit tests for CalendarTushare.list
 */
@SpringBootTest
@Slf4j
public class CalendarTushareTest {
    
    @Autowired
    private CalendarTushare calendarTushare;

    @Test
    public void listTest() {
        CalendarQuery query = new CalendarQuery();
        LocalDate now = LocalDate.now();
        List<Calendar> calendars = calendarTushare.list(query);
        for (Calendar calendar : calendars) {
            log.info("Calendar: {}", JSON.toJSONString(calendar));
        }
        log.info("calendar size: {}", calendars.size());
    }
}
