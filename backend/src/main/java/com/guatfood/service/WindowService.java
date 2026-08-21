package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.common.PageResult;
import com.guatfood.dto.WindowDTO;
import com.guatfood.entity.Window;

public interface WindowService extends IService<Window> {

    /** 排行榜 (type: overall/taste/hot/recent) */
    PageResult<Window> rank(String type, Long canteenId, String keyword, Integer page, Integer size);

    Window getDetail(Long id);

    Window submit(WindowDTO dto, Long userId);

    void approve(Long id, String status);

    void deleteWindow(Long id);

    /** 管理端列表 (含待审核) */
    Page<Window> adminList(String status, String keyword, Integer page, Integer size);
}
