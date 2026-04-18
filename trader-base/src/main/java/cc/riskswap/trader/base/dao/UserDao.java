package cc.riskswap.trader.base.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cc.riskswap.trader.base.dao.entity.User;
import cc.riskswap.trader.base.dao.mapper.UserMapper;

import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class UserDao extends ServiceImpl<UserMapper, User> {
    
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        return baseMapper.selectOne(q);
    }

    public int updateLastLogin(Integer id, OffsetDateTime time) {
        User u = new User();
        u.setId(id);
        u.setLastLogin(time);
        u.setUpdatedAt(time);
        return baseMapper.updateById(u);
    }

    public int updatePassword(Integer id, String hashed, OffsetDateTime time) {
        User u = new User();
        u.setId(id);
        u.setPassword(hashed);
        u.setUpdatedAt(time);
        return baseMapper.updateById(u);
    }
}
