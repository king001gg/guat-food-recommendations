package com.guatfood.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guatfood.common.PageResult;
import com.guatfood.entity.Canteen;
import com.guatfood.entity.Dish;
import com.guatfood.entity.Rating;
import com.guatfood.entity.User;
import com.guatfood.entity.Window;
import com.guatfood.mapper.CanteenMapper;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.RatingMapper;
import com.guatfood.mapper.UserMapper;
import com.guatfood.mapper.WindowMapper;
import com.guatfood.service.DishService;
import com.guatfood.service.StatService;
import com.guatfood.service.WindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatServiceImpl implements StatService {

    @Autowired
    private CanteenMapper canteenMapper;
    @Autowired
    private WindowMapper windowMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private RatingMapper ratingMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WindowService windowService;
    @Autowired
    private DishService dishService;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("userCount", userMapper.selectCount(null));
        overview.put("canteenCount", canteenMapper.selectCount(null));
        overview.put("windowCount", windowMapper.selectCount(
                new LambdaQueryWrapper<Window>().eq(Window::getStatus, "PUBLISHED")));
        overview.put("dishCount", dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, "PUBLISHED")));
        overview.put("ratingCount", ratingMapper.selectCount(null));
        overview.put("pendingCount", windowMapper.selectCount(
                        new LambdaQueryWrapper<Window>().eq(Window::getStatus, "PENDING"))
                + dishMapper.selectCount(
                        new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, "PENDING")));
        return overview;
    }

    @Override
    public List<Map<String, Object>> getCanteenStats() {
        List<Canteen> canteens = canteenMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Canteen canteen : canteens) {
            long count = windowMapper.selectCount(
                    new LambdaQueryWrapper<Window>().eq(Window::getCanteenId, canteen.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", canteen.getName());
            item.put("value", count);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopWindows(int limit) {
        PageResult<Window> rank = windowService.rank("overall", null, null, 1, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Window w : rank.getList()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", w.getName());
            item.put("score", w.getScoreAvg());
            item.put("ratingCount", w.getRatingCount());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopDishes(int limit) {
        PageResult<Dish> rank = dishService.rank("overall", null, null, null, 1, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dish d : rank.getList()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", d.getName());
            item.put("score", d.getScoreAvg());
            item.put("ratingCount", d.getRatingCount());
            result.add(item);
        }
        return result;
    }
}
