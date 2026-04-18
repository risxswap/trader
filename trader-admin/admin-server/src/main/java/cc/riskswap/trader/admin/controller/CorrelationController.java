package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.CorrelationDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.CorrelationParam;
import cc.riskswap.trader.admin.common.model.query.CorrelationQuery;
import cc.riskswap.trader.admin.service.CorrelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/correlation")
public class CorrelationController {

    @Autowired
    private CorrelationService correlationService;

    @GetMapping
    public ResData<PageDto<CorrelationDto>> list(CorrelationQuery query) {
        return ResData.success(correlationService.list(query));
    }

    @GetMapping("/{id}")
    public ResData<CorrelationDto> detail(@PathVariable Long id) {
        return ResData.success(correlationService.detail(id));
    }

    @PostMapping
    public ResData<Void> add(@RequestBody CorrelationParam param) {
        correlationService.add(param);
        return ResData.success(null);
    }

    @PutMapping
    public ResData<Void> update(@RequestBody CorrelationParam param) {
        correlationService.update(param);
        return ResData.success(null);
    }

    @DeleteMapping("/{id}")
    public ResData<Void> delete(@PathVariable Long id) {
        correlationService.delete(id);
        return ResData.success(null);
    }
}
