package cc.riskswap.trader.collector.task;

import org.springframework.stereotype.Component;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.CollectorTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.CalendarService;

@Component
public class CalendarSyncTask implements CollectorTask {

    private final CalendarService calendarService;

    public CalendarSyncTask(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Override
    public String getTaskCode() {
        return "collector.calendar.sync";
    }

    @Override
    public String getTaskName() {
        return "交易日历同步任务";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{}";
    }

    @Override
    public String getDefaultParams() {
        return "{}";
    }

    @Override
    @TraderTaskLog("交易日历同步任务")
    public void execute(TraderTaskContext context) {
        calendarService.syncCalendar(context);
    }
}
