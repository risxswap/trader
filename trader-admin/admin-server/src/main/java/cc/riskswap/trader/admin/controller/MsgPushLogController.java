package cc.riskswap.trader.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.MsgPushLogDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.MsgPushParam;
import cc.riskswap.trader.admin.common.model.query.MsgPushLogQuery;
import cc.riskswap.trader.admin.service.MsgPushLogService;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/msg-push-log")
public class MsgPushLogController {

    @Resource
    private MsgPushLogService msgPushLogService;

    @GetMapping
    public ResData<PageDto<MsgPushLogDto>> list(MsgPushLogQuery query) {
        return ResData.success(msgPushLogService.list(query));
    }

    @GetMapping("/{id}")
    public ResData<MsgPushLogDto> get(@PathVariable Integer id) {
        return ResData.success(msgPushLogService.getDetail(id));
    }

    @PostMapping("/send")
    public ResData<Void> send(@RequestBody MsgPushParam param) {
        msgPushLogService.send(param);
        return ResData.success(null);
    }
}
