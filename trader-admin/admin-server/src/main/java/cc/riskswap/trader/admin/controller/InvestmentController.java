package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.InvestmentDto;
import cc.riskswap.trader.admin.common.model.param.InvestmentParam;
import cc.riskswap.trader.admin.common.model.query.InvestmentQuery;
import cc.riskswap.trader.admin.service.InvestmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 投资控制器
 */
@RestController
@RequestMapping("/investments")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    @GetMapping
    public ResData<PageDto<InvestmentDto>> list(InvestmentQuery query) {
        return ResData.success(investmentService.list(query));
    }

    @PostMapping
    public ResData<Void> add(@RequestBody InvestmentParam param) {
        investmentService.add(param);
        return ResData.success(null);
    }
    
    @PutMapping
    public ResData<Void> update(@RequestBody InvestmentParam param) {
        investmentService.update(param);
        return ResData.success(null);
    }
    
    @DeleteMapping("/{id}")
    public ResData<Void> delete(@PathVariable Integer id) {
        investmentService.delete(id);
        return ResData.success(null);
    }

    @GetMapping("/{id}")
    public ResData<InvestmentDto> get(@PathVariable Integer id) {
        return ResData.success(investmentService.get(id));
    }
}
