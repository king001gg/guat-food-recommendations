package com.guatfood.controller;

import com.guatfood.common.Result;
import com.guatfood.dto.LoginDTO;
import com.guatfood.dto.RegisterDTO;
import com.guatfood.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", userService.register(dto));
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO dto) {
        Map<String, Object> result = userService.login(dto);
        return Result.success("登录成功", result);
    }
}
