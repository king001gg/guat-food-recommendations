package com.guatfood.service;

import java.util.List;
import java.util.Map;

public interface StatService {

    Map<String, Object> getOverview();

    /** 各食堂档口数量 (饼图) */
    List<Map<String, Object>> getCanteenStats();

    /** 综合榜前 N 名档口 (柱状图) */
    List<Map<String, Object>> getTopWindows(int limit);

    /** 综合榜前 N 名菜品 (柱状图) */
    List<Map<String, Object>> getTopDishes(int limit);
}
