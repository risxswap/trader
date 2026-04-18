package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String token;
    private String username;
    private String nickname;
}
