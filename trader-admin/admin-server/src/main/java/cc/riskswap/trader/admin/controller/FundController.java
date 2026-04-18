package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.FundAdjDto;
import cc.riskswap.trader.admin.common.model.dto.FundDto;
import cc.riskswap.trader.admin.common.model.dto.FundMarketDto;
import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.param.FundUpdateParam;
import cc.riskswap.trader.admin.common.model.query.FundAdjListQuery;
import cc.riskswap.trader.admin.common.model.query.FundAdjQuery;
import cc.riskswap.trader.admin.common.model.query.FundListQuery;
import cc.riskswap.trader.admin.common.model.query.FundMarketListQuery;
import cc.riskswap.trader.admin.common.model.query.FundMarketQuery;
import cc.riskswap.trader.admin.dao.FundAdjDao;
import cc.riskswap.trader.admin.dao.FundDao;
import cc.riskswap.trader.admin.service.FundMarketService;
import cc.riskswap.trader.admin.service.FundService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fund")
public class FundController {
    @Resource
    private FundDao fundDao;
    @Resource
    private FundMarketService fundMarketService;
    @Resource
    private FundAdjDao fundAdjDao;
    @Resource
    private FundService fundService;

    @PostMapping("/list")
    public ResData<PageDto<FundDto>> list(@Valid @RequestBody FundListQuery q) {
        PageDto<FundDto> res = fundMarketService == null ? null : fundService.listFunds(q);
        return ResData.success(res);
    }

    @GetMapping("/detail/{code}")
    public ResData<FundDto> detail(@PathVariable String code) {
        FundDto f = fundService.getDetail(code);
        return ResData.success(f);
    }

    @PutMapping("/update/{code}")
    public ResData<Void> update(@PathVariable String code, @Valid @RequestBody FundUpdateParam p) {
        boolean ok = fundService.updateFundBasic(code, p);
        if (!ok) {
            return ResData.error(400, "更新失败");
        }
        return ResData.success();
    }

    @DeleteMapping("/delete/{code}")
    public ResData<Void> delete(@PathVariable String code) {
        boolean ok = fundService.deleteFund(code);
        if (!ok) {
            return ResData.error(404, "记录不存在");
        }
        return ResData.success();
    }
    @PostMapping("/market")
    public ResData<List<FundMarketDto>> market(@Valid @RequestBody FundMarketQuery q) {
        List<FundMarketDto> result = fundService.getMarket(q);
        return ResData.success(result);
    }

    @PostMapping("/adj")
    public ResData<List<FundAdjDto>> adj(@Valid @RequestBody FundAdjQuery q) {
        List<FundAdjDto> result = fundService.getAdj(q);
        return ResData.success(result);
    }

    @GetMapping("/default-code")
    public ResData<String> defaultCode() {
        String code = fundService.getDefaultFundCode();
        return ResData.success(code);
    }

    @PostMapping("/market/list")
    public ResData<PageDto<FundMarketDto>> marketList(@Valid @RequestBody FundMarketListQuery q) {
        PageDto<FundMarketDto> res = fundService.listFundMarkets(q);
        return ResData.success(res);
    }

    @PostMapping("/adj/list")
    public ResData<PageDto<FundAdjDto>> adjList(@Valid @RequestBody FundAdjListQuery q) {
        PageDto<FundAdjDto> res = fundService.listFundAdjs(q);
        return ResData.success(res);
    }
}
