package com.guatfood.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.config.JwtConfig;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.entity.User;
import com.guatfood.mapper.UserMapper;
import com.guatfood.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtConfig jwtConfig;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户不存在时也执行一次 BCrypt 校验，避免通过响应耗时差异枚举账号。
     * 该哈希只用于等时比较，不对应任何真实账号。
     */
    private static final String DUMMY_PASSWORD_HASH =
            new BCryptPasswordEncoder().encode("timing-equalization-placeholder");

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
        user.setTokenVersion(0);
        try {
            save(user);
        } catch (DataIntegrityViolationException e) {
            // 并发注册时唯一索引兜底，避免暴露 500
            throw new BusinessException(409, "用户名已被注册");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        User user = getByUsername(dto.getUsername());
        String storedHash = user != null && user.getPassword() != null
                ? user.getPassword() : DUMMY_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(dto.getPassword(), storedHash);
        if (user == null || !passwordMatches) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String token = jwtConfig.generateToken(user.getId(), user.getUsername(), tokenVersion);
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public Page<User> getUserPage(Integer page, Integer size, String keyword) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = page(new Page<>(safePage, safeSize), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public void logout(Long userId) {
        if (userId != null) {
            baseMapper.incrementTokenVersion(userId);
        }
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        requireStrongEnough(newPassword);
        baseMapper.updatePasswordAndIncrementVersion(userId, passwordEncoder.encode(newPassword));
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        requireStrongEnough(newPassword);
        baseMapper.updatePasswordAndIncrementVersion(id, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException("非法状态");
        }
        User user = getById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if ("ADMIN".equals(user.getRole()) && !"ACTIVE".equals(status)) {
            throw new BusinessException(409, "不能禁用管理员账号");
        }
        User upd = new User();
        upd.setId(id);
        upd.setStatus(status);
        updateById(upd);
        // 禁用时吊销该用户全部旧 Token；重新启用后必须重新登录
        if ("DISABLED".equals(status)) {
            baseMapper.incrementTokenVersion(id);
        }
    }

    @Override
    @Transactional
    public void updateRole(Long id, String role) {
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            throw new BusinessException("非法角色");
        }
        User user = getById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if ("ADMIN".equals(user.getRole()) && "ACTIVE".equals(user.getStatus()) && "USER".equals(role)) {
            long activeAdmins = count(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN")
                    .eq(User::getStatus, "ACTIVE"));
            if (activeAdmins <= 1) {
                throw new BusinessException(409, "不能取消最后一个管理员");
            }
        }
        User upd = new User();
        upd.setId(id);
        upd.setRole(role);
        updateById(upd);
    }

    private void requireStrongEnough(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException(400, "密码至少需要 8 位");
        }
        if (password.length() > 100) {
            throw new BusinessException(400, "密码最多 100 位");
        }
    }
}
