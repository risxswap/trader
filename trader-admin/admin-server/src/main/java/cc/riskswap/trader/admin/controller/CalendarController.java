package cc.riskswap.trader.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.CalendarDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.query.CalendarListQuery;
import cc.riskswap.trader.admin.service.CalendarService;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    @GetMapping("/list")
    public ResData<PageDto<CalendarDto>> list(CalendarListQuery q) {
        return ResData.success(calendarService.list(q));
    }
}
