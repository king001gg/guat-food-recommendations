package com.guatfood.controller;

import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.entity.Canteen;
import com.guatfood.service.CanteenService;
import com.guatfood.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/canteens")
public class CanteenController {

    @Autowired
    private CanteenService canteenService;
    @Autowired
    private UserService userService;

    @GetMapping
    public Result<List<Canteen>> list() {
        return Result.success(canteenService.listAll());
    }

    @PostMapping
    public Result<Canteen> create(@RequestBody Canteen canteen) {
        AuthUtil.requireAdmin(userService);
        return Result.success(canteenService.create(canteen));
    }

    @PutMapping("/{id}")
    public Result<Canteen> update(@PathVariable Long id, @RequestBody Canteen canteen) {
        AuthUtil.requireAdmin(userService);
        canteen.setId(id);
        return Result.success(canteenService.update(canteen));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        AuthUtil.requireAdmin(userService);
        canteenService.delete(id);
        return Result.success();
    }
}
