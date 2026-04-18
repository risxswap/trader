package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.InvestmentLogDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.InvestmentLogParam;
import cc.riskswap.trader.admin.common.model.query.InvestmentLogQuery;
import cc.riskswap.trader.admin.service.InvestmentLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 投资日志控制器
 */
@RestController
@RequestMapping("/investment-logs")
public class InvestmentLogController {

    @Autowired
    private InvestmentLogService investmentLogService;

    @GetMapping
    public ResData<PageDto<InvestmentLogDto>> list(InvestmentLogQuery query) {
        return ResData.success(investmentLogService.list(query));
    }

    @PostMapping
    public ResData<Void> add(@RequestBody InvestmentLogParam param) {
        investmentLogService.add(param);
        return ResData.success(null);
    }
    
    @PutMapping
    public ResData<Void> update(@RequestBody InvestmentLogParam param) {
        investmentLogService.update(param);
        return ResData.success(null);
    }
    
    @DeleteMapping("/{id}")
    public ResData<Void> delete(@PathVariable Integer id) {
        investmentLogService.delete(id);
        return ResData.success(null);
    }
}
