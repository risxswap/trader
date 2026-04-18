package cc.riskswap.trader.executor.common.model;

import lombok.Data;

@Data
public class LoginDto {
    private String token;
    private String username;
    private String nickname;
}
