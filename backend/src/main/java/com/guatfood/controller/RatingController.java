package com.guatfood.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.dto.RatingDTO;
import com.guatfood.entity.Rating;
import com.guatfood.service.RatingService;
import com.guatfood.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;
    @Autowired
    private UserService userService;

    /** 提交/更新评分 */
    @PostMapping
    public Result<Rating> submit(@Valid @RequestBody RatingDTO dto) {
        Long userId = AuthUtil.requireLogin();
        return Result.success("评价成功", ratingService.submit(dto, userId));
    }

    /** 某目标的评分列表 */
    @GetMapping
    public Result<Page<Rating>> listByTarget(@RequestParam String targetType,
                                             @RequestParam Long targetId,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(ratingService.listByTarget(targetType, targetId, page, size));
    }

    /** 管理端评分列表 */
    @GetMapping("/all")
    public Result<Page<Rating>> adminList(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        AuthUtil.requireAdmin(userService);
        return Result.success(ratingService.adminList(page, size, keyword));
    }

    /** 我的评分 */
    @GetMapping("/mine")
    public Result<Page<Rating>> myRatings(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        Long userId = AuthUtil.requireLogin();
        return Result.success(ratingService.myRatings(userId, page, size));
    }

    /** 删除自己的评分 */
    @DeleteMapping("/{id}")
    public Result<?> deleteMine(@PathVariable Long id) {
        Long userId = AuthUtil.requireLogin();
        ratingService.deleteMine(id, userId);
        return Result.success("删除成功");
    }

    /** 管理员删除评分 */
    @DeleteMapping("/admin/{id}")
    public Result<?> adminDelete(@PathVariable Long id) {
        AuthUtil.requireAdmin(userService);
        ratingService.adminDelete(id);
        return Result.success("删除成功");
    }
}
