package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    User register(RegisterDTO dto);

    Map<String, Object> login(LoginDTO dto);

    Page<User> getUserPage(Integer page, Integer size, String keyword);

    User getByUsername(String username);

    /** 服务端注销：递增 token_version */
    void logout(Long userId);

    /** 用户修改自己的密码；成功后旧 Token 全部失效 */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 管理员重置指定用户密码；成功后该用户旧 Token 全部失效 */
    void resetPassword(Long id, String newPassword);

    void updateStatus(Long id, String status);

    void updateRole(Long id, String role);
}
