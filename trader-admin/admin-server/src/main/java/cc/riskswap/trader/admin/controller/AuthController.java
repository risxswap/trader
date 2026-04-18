package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.LoginDto;
import cc.riskswap.trader.admin.common.model.param.LoginParam;
import cc.riskswap.trader.admin.service.AuthService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public ResData<LoginDto> login(@Valid @RequestBody LoginParam req) {
        LoginDto r = authService.login(req);
        if (r == null) {
            return ResData.error(ErrorCode.UNAUTHORIZED.code(), "用户名或密码错误");
        }
        return ResData.success(r);
    }
}
