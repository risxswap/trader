package cc.riskswap.trader.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.FundNavDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.query.FundNavListQuery;
import cc.riskswap.trader.admin.service.FundNavService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fund-nav")
public class FundNavController {

    @Autowired
    private FundNavService fundNavService;

    @PostMapping("/list")
    public ResData<PageDto<FundNavDto>> list(@Valid @RequestBody FundNavListQuery q) {
        PageDto<FundNavDto> res = fundNavService.listNav(q);
        return ResData.success(res);
    }
}
