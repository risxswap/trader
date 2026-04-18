package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.TaskLogDto;
import cc.riskswap.trader.admin.common.model.query.TaskLogQuery;
import cc.riskswap.trader.admin.service.TaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs/task")
@RequiredArgsConstructor
public class TaskLogController {
    private final TaskLogService taskLogService;

    @GetMapping
    public ResData<PageDto<TaskLogDto>> list(TaskLogQuery query) {
        return ResData.success(taskLogService.list(query));
    }

    @GetMapping("/{id}")
    public ResData<TaskLogDto> get(@PathVariable Long id) {
        return ResData.success(taskLogService.getDetail(id));
    }
}
