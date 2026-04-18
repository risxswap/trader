package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.SystemTaskDto;
import cc.riskswap.trader.admin.common.model.dto.TaskDefinitionDto;
import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceCreateParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceDeleteParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskTriggerParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam;
import cc.riskswap.trader.admin.common.model.query.SystemTaskListQuery;
import cc.riskswap.trader.admin.common.model.query.TaskDefinitionListQuery;
import cc.riskswap.trader.admin.service.SystemTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class SystemTaskController {
    private final SystemTaskService systemTaskService;

    @PostMapping("/list")
    public ResData<PageDto<SystemTaskDto>> list(@RequestBody SystemTaskListQuery query) {
        return ResData.success(systemTaskService.list(query));
    }

    @PostMapping("/definition/list")
    public ResData<List<TaskDefinitionDto>> definitions(@RequestBody TaskDefinitionListQuery query) {
        return ResData.success(systemTaskService.definitions(query));
    }

    @PostMapping("/instance/create")
    public ResData<Void> createInstance(@Valid @RequestBody SystemTaskInstanceCreateParam param) {
        systemTaskService.createInstance(param);
        return ResData.success();
    }

    @PostMapping("/instance/delete")
    public ResData<Void> deleteInstance(@RequestBody SystemTaskInstanceDeleteParam param) {
        systemTaskService.deleteInstance(param);
        return ResData.success();
    }

    @PostMapping("/update")
    public ResData<Void> update(@RequestBody SystemTaskUpdateParam param) {
        systemTaskService.update(param);
        return ResData.success();
    }

    @PostMapping("/trigger")
    public ResData<Void> trigger(@RequestBody SystemTaskTriggerParam param) {
        systemTaskService.trigger(param);
        return ResData.success();
    }
}
