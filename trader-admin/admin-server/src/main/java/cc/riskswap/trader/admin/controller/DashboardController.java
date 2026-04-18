package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.DashboardDto;
import cc.riskswap.trader.admin.common.model.dto.SystemStatusDto;
import cc.riskswap.trader.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResData<DashboardDto> getOverview() {
        return ResData.success(dashboardService.getOverview());
    }

    @GetMapping("/system-status")
    public ResData<SystemStatusDto> getSystemStatus() {
        return ResData.success(dashboardService.getSystemStatus());
    }
}
