package com.guatfood.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.config.JwtConfig;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.entity.User;
import com.guatfood.mapper.UserMapper;
import com.guatfood.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtConfig jwtConfig;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User register(RegisterDTO dto) {
        if (getByUsername(dto.getUsername()) != null) {
            throw new BusinessException(409, "用户名已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setAvatar(null);
        user.setRole("USER");
        user.setStatus("ACTIVE");
        save(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        User user = getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        String token = jwtConfig.generateToken(user.getId(), user.getUsername());
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public Page<User> getUserPage(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public void updateStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException("非法状态");
        }
        User user = getById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if ("ADMIN".equals(user.getRole()) && !"ACTIVE".equals(status)) {
            throw new BusinessException("不能禁用管理员账号");
        }
        User upd = new User();
        upd.setId(id);
        upd.setStatus(status);
        updateById(upd);
    }

    @Override
    public void updateRole(Long id, String role) {
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            throw new BusinessException("非法角色");
        }
        User user = getById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        User upd = new User();
        upd.setId(id);
        upd.setRole(role);
        updateById(upd);
    }
}
