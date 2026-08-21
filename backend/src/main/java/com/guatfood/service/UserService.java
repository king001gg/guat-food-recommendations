package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    User register(RegisterDTO dto);

    Map<String, Object> login(LoginDTO dto);

    Page<User> getUserPage(Integer page, Integer size, String keyword);

    User getByUsername(String username);

    void updateStatus(Long id, String status);

    void updateRole(Long id, String role);
}
