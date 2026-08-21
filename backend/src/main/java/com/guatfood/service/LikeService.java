package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.entity.Like;

import java.util.Map;

public interface LikeService extends IService<Like> {

    boolean toggle(Long userId, String targetType, Long targetId);

    boolean isLiked(Long userId, String targetType, Long targetId);

    long getCount(String targetType, Long targetId);

    /** 某类型所有目标的点赞数: targetId -> count */
    Map<Long, Long> countByTargets(String targetType);

    Page<Like> myLikes(Long userId, Integer page, Integer size);
}
