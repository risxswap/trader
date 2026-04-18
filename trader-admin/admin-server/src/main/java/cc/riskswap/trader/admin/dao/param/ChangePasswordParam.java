package cc.riskswap.trader.admin.dao.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordParam {
    @NotBlank
    private String username;
    @NotBlank
    private String oldPassword;
    @NotBlank
    private String newPassword;
}
