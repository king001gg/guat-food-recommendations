package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.common.PageResult;
import com.guatfood.dto.DishDTO;
import com.guatfood.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    /** 排行榜 (type: overall/taste/hot/recent) */
    PageResult<Dish> rank(String type, Long windowId, Long canteenId, String keyword, Integer page, Integer size);

    Dish getDetail(Long id);

    List<Dish> listByWindow(Long windowId);

    Dish submit(DishDTO dto, Long userId);

    void approve(Long id, String status);

    void deleteDish(Long id);

    /** 管理端列表 (含待审核) */
    Page<Dish> adminList(String status, String keyword, Integer page, Integer size);
}
