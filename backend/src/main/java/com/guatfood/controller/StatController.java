package com.guatfood.controller;

import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.service.StatService;
import com.guatfood.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    @Autowired
    private StatService statService;
    @Autowired
    private UserService userService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        AuthUtil.requireAdmin(userService);
        return Result.success(statService.getOverview());
    }

    @GetMapping("/canteen")
    public Result<List<Map<String, Object>>> canteenStats() {
        AuthUtil.requireAdmin(userService);
        return Result.success(statService.getCanteenStats());
    }

    @GetMapping("/top-windows")
    public Result<List<Map<String, Object>>> topWindows(@RequestParam(defaultValue = "10") int limit) {
        AuthUtil.requireAdmin(userService);
        return Result.success(statService.getTopWindows(limit));
    }

    @GetMapping("/top-dishes")
    public Result<List<Map<String, Object>>> topDishes(@RequestParam(defaultValue = "10") int limit) {
        AuthUtil.requireAdmin(userService);
        return Result.success(statService.getTopDishes(limit));
    }
}
