package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.entity.Favorite;

import java.util.Map;

public interface FavoriteService extends IService<Favorite> {

    boolean toggle(Long userId, String targetType, Long targetId);

    boolean isFavorited(Long userId, String targetType, Long targetId);

    /** 某类型所有目标的收藏数: targetId -> count */
    Map<Long, Long> countByTargets(String targetType);

    Page<Favorite> myFavorites(Long userId, Integer page, Integer size);
}
