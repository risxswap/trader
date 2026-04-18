package cc.riskswap.trader.admin.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cc.riskswap.trader.admin.common.model.dto.CalendarDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.ResultSetDto;
import cc.riskswap.trader.admin.common.model.query.CalendarListQuery;
import cc.riskswap.trader.admin.common.util.DateUtil;
import cc.riskswap.trader.admin.dao.CalendarDao;
import cc.riskswap.trader.admin.dao.entity.Calendar;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class CalendarService {

    @Autowired
    private CalendarDao calendarDao;

    public PageDto<CalendarDto> list(CalendarListQuery q) {
        cc.riskswap.trader.admin.dao.query.CalendarListQuery listQuery = new cc.riskswap.trader.admin.dao.query.CalendarListQuery();
        BeanUtil.copyProperties(q, listQuery);
        Page<Calendar> p = calendarDao.pageQuery(listQuery);
        List<CalendarDto> list = p.getRecords().stream()
                .map(c -> BeanUtil.copyProperties(c, CalendarDto.class))
                .collect(Collectors.toList());
        
        PageDto<CalendarDto> res = new PageDto<>();
        res.setItems(list);
        res.setTotal(p.getTotal());
        res.setPageNo(q.getPageNo());
        res.setPageSize(q.getPageSize());
        return res;
    }

    /**
     * 查询日历列表
     * @param year
     * @return
     */
    /**
     * 获取指定年份的中国日历数据
     * 
     * @param year 年份
     * @return 包含日期和是否开市信息的ResultSetDto
     */
    public ResultSetDto getCalendarCN(int year) {
        // 获取数据库中的日历数据
        List<Calendar> calendars = calendarDao.list(year);
        Map<String, Calendar> calendarMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(calendars)) {
            for (Calendar calendar: calendars) {
                String date = calendar.getDate().format(DateUtil.D_FORMAT_INT_FORMATTER);
                calendarMap.put(date, calendar);
            }
        }
        
        // 创建结果集
        ResultSetDto resultSetDto = new ResultSetDto();
        resultSetDto.setFields(new String[]{"date", "is_open"});
        
        // 生成一整年的日期
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        // 计算天数
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        String[][] items = new String[days][2];
        
        // 填充数据
        for (int i = 0; i < days; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            String dateStr = currentDate.format(DateUtil.D_FORMAT_INT_FORMATTER);
            
            // 判断是否开市
            int isOpen;
            if (calendarMap.containsKey(dateStr)) {
                // 如果在日历数据中存在，使用数据库中的值
                isOpen = calendarMap.get(dateStr).getOpen();
            } else {
                // 如果不在日历数据中，周六周日设为0，其他设为1
                DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
                isOpen = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) ? 0 : 1;
            }
            
            items[i][0] = dateStr;
            items[i][1] = String.valueOf(isOpen);
        }
        
        resultSetDto.setItems(items);
        return resultSetDto;
    }
}
