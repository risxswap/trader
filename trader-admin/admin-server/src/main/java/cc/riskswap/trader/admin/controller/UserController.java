package cc.riskswap.trader.admin.controller;

import cc.riskswap.trader.admin.common.model.ErrorCode;
import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.param.ChangePasswordParam;
import cc.riskswap.trader.admin.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/change-password")
    public ResData<Void> changePassword(@Valid @RequestBody ChangePasswordParam req) {
        boolean ok = userService.changePassword(req);
        if (!ok) {
            return ResData.error(ErrorCode.BAD_REQUEST.code(), "密码修改失败");
        }
        return ResData.success();
    }
}
