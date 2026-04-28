package cc.riskswap.trader.collector.test.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.collector.service.CalendarService;
import cc.riskswap.trader.base.task.TraderTaskContext;

@SpringBootTest
public class CalendarServiceTest {

    @Autowired
    private CalendarService calendarService;
    
    @Test
    public void testSyncByExchange() {
        calendarService.syncCalendar(new TraderTaskContext());
    }
}
