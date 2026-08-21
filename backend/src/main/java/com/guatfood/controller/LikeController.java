package com.guatfood.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guatfood.common.AuthUtil;
import com.guatfood.common.Result;
import com.guatfood.dto.TargetDTO;
import com.guatfood.entity.Like;
import com.guatfood.service.LikeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /** 点赞/取消点赞 */
    @PostMapping
    public Result<?> toggle(@Valid @RequestBody TargetDTO dto) {
        Long userId = AuthUtil.requireLogin();
        boolean liked = likeService.toggle(userId, dto.getTargetType(), dto.getTargetId());
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("count", likeService.getCount(dto.getTargetType(), dto.getTargetId()));
        return Result.success(liked ? "已点赞" : "已取消点赞", data);
    }

    /** 我的点赞列表 */
    @GetMapping("/mine")
    public Result<Page<Like>> myLikes(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "12") Integer size) {
        Long userId = AuthUtil.requireLogin();
        return Result.success(likeService.myLikes(userId, page, size));
    }
}
