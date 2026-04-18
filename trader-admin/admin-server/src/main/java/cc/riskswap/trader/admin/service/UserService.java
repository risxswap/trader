package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.param.ChangePasswordParam;
import cc.riskswap.trader.admin.dao.UserDao;
import cc.riskswap.trader.admin.dao.entity.User;
import jakarta.annotation.Resource;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class UserService {
    @Resource
    private UserDao userDao;

    public void initAdmin() {
        User existing = userDao.getByUsername("admin");
        if (existing != null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        User u = new User();
        u.setUsername("admin");
        u.setPassword(BCrypt.hashpw("Admin123!!!", BCrypt.gensalt()));
        u.setNickname("Admin");
        u.setLocked(false);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        userDao.save(u);
    }

    public boolean changePassword(ChangePasswordParam req) {
        User user = userDao.getByUsername(req.getUsername());
        if (user == null) {
            return false;
        }
        if (Boolean.TRUE.equals(user.getLocked())) {
            return false;
        }
        boolean ok = BCrypt.checkpw(req.getOldPassword(), user.getPassword());
        if (!ok) {
            return false;
        }
        String hashed = BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt());
        int n = userDao.updatePassword(user.getId(), hashed, OffsetDateTime.now());
        return n > 0;
    }
}
