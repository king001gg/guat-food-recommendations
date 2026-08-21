package com.guatfood.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.PageResult;
import com.guatfood.common.Result;
import com.guatfood.dto.WindowDTO;
import com.guatfood.entity.Window;
import com.guatfood.service.UserService;
import com.guatfood.service.WindowService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/windows")
public class WindowController {

    @Autowired
    private WindowService windowService;
    @Autowired
    private UserService userService;

    /** 排行榜 (type: overall/taste/hot/recent) */
    @GetMapping
    public Result<PageResult<Window>> rank(@RequestParam(defaultValue = "overall") String type,
                                           @RequestParam(required = false) Long canteenId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(windowService.rank(type, canteenId, keyword, page, size));
    }

    /** 管理端列表 (含待审核) */
    @GetMapping("/all")
    public Result<Page<Window>> adminList(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        AuthUtil.requireAdmin(userService);
        return Result.success(windowService.adminList(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Window> detail(@PathVariable Long id) {
        return Result.success(windowService.getDetail(id));
    }

    /** 投稿档口 (普通用户提交为待审核) */
    @PostMapping
    public Result<Window> submit(@Valid @RequestBody WindowDTO dto) {
        Long userId = AuthUtil.requireLogin();
        return Result.success("投稿成功，等待审核", windowService.submit(dto, userId));
    }

    /** 审核档口 */
    @PutMapping("/{id}/status")
    public Result<?> approve(@PathVariable Long id, @RequestParam String status) {
        AuthUtil.requireAdmin(userService);
        windowService.approve(id, status);
        return Result.success("审核完成");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        AuthUtil.requireAdmin(userService);
        windowService.deleteWindow(id);
        return Result.success("删除成功");
    }
}
