package com.guatfood.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.dto.TargetDTO;
import com.guatfood.entity.Favorite;
import com.guatfood.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /** 收藏/取消收藏 */
    @PostMapping
    public Result<?> toggle(@Valid @RequestBody TargetDTO dto) {
        Long userId = AuthUtil.requireLogin();
        boolean favorited = favoriteService.toggle(userId, dto.getTargetType(), dto.getTargetId());
        return Result.success(favorited ? "已收藏" : "已取消收藏", favorited);
    }

    /** 我的收藏列表 */
    @GetMapping("/mine")
    public Result<Page<Favorite>> myFavorites(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "12") Integer size) {
        Long userId = AuthUtil.requireLogin();
        return Result.success(favoriteService.myFavorites(userId, page, size));
    }
}
