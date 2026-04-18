package cc.riskswap.trader.admin.common.model.param;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class LoginParam {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
