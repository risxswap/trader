package cc.riskswap.trader.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.InvestmentPositionDto;
import cc.riskswap.trader.admin.common.model.query.InvestmentPositionListQuery;
import cc.riskswap.trader.admin.service.InvestmentPositionService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import cc.riskswap.trader.admin.common.model.param.InvestmentPositionParam;
import cc.riskswap.trader.admin.common.model.param.InvestmentPositionUpdateParam;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/investment-position")
public class InvestmentPositionController {

    @Autowired
    private InvestmentPositionService investmentPositionService;

    @PostMapping("/add")
    public ResData<Void> add(@RequestBody @Valid InvestmentPositionParam param) {
        investmentPositionService.add(param);
        return ResData.success(null);
    }

    @PostMapping("/update")
    public ResData<Void> update(@RequestBody @Valid InvestmentPositionUpdateParam param) {
        investmentPositionService.update(param);
        return ResData.success(null);
    }

    @PostMapping("/delete")
    public ResData<Void> delete(@RequestParam Integer id) {
        investmentPositionService.delete(id);
        return ResData.success(null);
    }

    @GetMapping("/list")
    public ResData<PageDto<InvestmentPositionDto>> list(InvestmentPositionListQuery q) {
        return ResData.success(investmentPositionService.list(q));
    }
}
