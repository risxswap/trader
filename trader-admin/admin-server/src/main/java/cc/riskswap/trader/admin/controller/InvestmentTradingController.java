package cc.riskswap.trader.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.InvestmentTradingDto;
import cc.riskswap.trader.admin.common.model.query.InvestmentTradingListQuery;
import cc.riskswap.trader.admin.service.InvestmentTradingService;

@RestController
@RequestMapping("/investment-trading")
public class InvestmentTradingController {

    @Autowired
    private InvestmentTradingService investmentTradingService;

    @GetMapping("/list")
    public ResData<PageDto<InvestmentTradingDto>> list(InvestmentTradingListQuery q) {
        return ResData.success(investmentTradingService.list(q));
    }
}
