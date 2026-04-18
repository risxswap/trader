package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.BrokerDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.BrokerParam;
import cc.riskswap.trader.admin.common.model.query.BrokerListQuery;
import cc.riskswap.trader.admin.service.BrokerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/broker")
public class BrokerController {

    @Resource
    private BrokerService brokerService;

    @GetMapping
    public ResData<PageDto<BrokerDto>> list(BrokerListQuery query) {
        return ResData.success(brokerService.list(query));
    }

    @PostMapping
    public ResData<Void> add(@Valid @RequestBody BrokerParam param) {
        brokerService.add(param);
        return ResData.success();
    }

    @PutMapping
    public ResData<Void> update(@Valid @RequestBody BrokerParam param) {
        brokerService.update(param);
        return ResData.success();
    }

    @DeleteMapping("/{id}")
    public ResData<Void> delete(@PathVariable Integer id) {
        brokerService.delete(id);
        return ResData.success();
    }
    
    @GetMapping("/{id}")
    public ResData<BrokerDto> get(@PathVariable Integer id) {
        return ResData.success(brokerService.get(id));
    }
}
