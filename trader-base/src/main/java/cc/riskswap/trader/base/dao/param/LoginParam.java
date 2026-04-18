package cc.riskswap.trader.base.dao.param;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class LoginParam {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
