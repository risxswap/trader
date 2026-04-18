package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.LoginDto;
import cc.riskswap.trader.admin.common.model.param.LoginParam;
import cc.riskswap.trader.admin.dao.UserDao;
import cc.riskswap.trader.admin.dao.entity.User;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.annotation.Resource;

@Service
public class AuthService {
    @Resource
    private UserDao userDao;

    public LoginDto login(LoginParam req) {
        User user = userDao.getByUsername(req.getUsername());
        if (user == null) {
            return null;
        }
        if (Boolean.TRUE.equals(user.getLocked())) {
            return null;
        }
        boolean ok = BCrypt.checkpw(req.getPassword(), user.getPassword());
        if (!ok) {
            return null;
        }
        userDao.updateLastLogin(user.getId(), OffsetDateTime.now());
        LoginDto res = new LoginDto();
        res.setToken(UUID.randomUUID().toString());
        res.setUsername(user.getUsername());
        res.setNickname(user.getNickname());
        return res;
    }

}
