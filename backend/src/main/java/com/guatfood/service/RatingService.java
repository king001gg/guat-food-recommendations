package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.dto.RatingDTO;
import com.guatfood.entity.Rating;

import java.util.Map;

public interface RatingService extends IService<Rating> {

    /** 提交/更新评分 (同一用户对同一目标仅一条) */
    Rating submit(RatingDTO dto, Long userId);

    Page<Rating> listByTarget(String targetType, Long targetId, Integer page, Integer size);

    Page<Rating> myRatings(Long userId, Integer page, Integer size);

    Page<Rating> adminList(Integer page, Integer size, String keyword);

    void deleteMine(Long id, Long userId);

    void adminDelete(Long id);

    /** 按目标聚合统计: targetId -> {ratingCount, tasteAvg, valueAvg, portionAvg, scoreAvg, recentCount} */
    Map<Long, Map<String, Object>> aggregateByTarget(String targetType);
}
