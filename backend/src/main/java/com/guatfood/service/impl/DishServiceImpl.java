package com.guatfood.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.common.PageResult;
import com.guatfood.common.PageUtil;
import com.guatfood.config.JwtInterceptor;
import com.guatfood.dto.DishDTO;
import com.guatfood.entity.Dish;
import com.guatfood.entity.User;
import com.guatfood.entity.Window;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.UserMapper;
import com.guatfood.mapper.WindowMapper;
import com.guatfood.service.DishService;
import com.guatfood.service.FavoriteService;
import com.guatfood.service.LikeService;
import com.guatfood.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private WindowMapper windowMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FavoriteService favoriteService;

    @Override
    public PageResult<Dish> rank(String type, Long windowId, Long canteenId, String keyword,
                                 Integer page, Integer size) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStatus, "PUBLISHED");
        if (windowId != null) wrapper.eq(Dish::getWindowId, windowId);
        if (canteenId != null) {
            List<Long> windowIds = windowMapper.selectList(
                            new LambdaQueryWrapper<Window>().eq(Window::getCanteenId, canteenId)).stream()
                    .map(Window::getId).toList();
            if (windowIds.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            wrapper.in(Dish::getWindowId, windowIds);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Dish::getName, keyword).or().like(Dish::getDescription, keyword));
        }
        List<Dish> all = list(wrapper);
        applyStats(all);
        sort(all, type);
        return PageUtil.paginate(all, page, size);
    }

    @Override
    public Dish getDetail(Long id) {
        Dish dish = getById(id);
        if (dish == null) throw new BusinessException(404, "菜品不存在");

        // 未发布的菜品仅管理员可见（防止绕过审核直接访问）
        if (!"PUBLISHED".equals(dish.getStatus())) {
            Long uid = JwtInterceptor.getCurrentUserId();
            User u = uid == null ? null : userMapper.selectById(uid);
            if (u == null || !"ADMIN".equals(u.getRole())) {
                throw new BusinessException(404, "菜品不存在或未发布");
            }
        }

        Dish upd = new Dish();
        upd.setId(id);
        upd.setViewCount((dish.getViewCount() == null ? 0 : dish.getViewCount()) + 1);
        updateById(upd);
        dish.setViewCount(upd.getViewCount());

        applyStats(List.of(dish));
        fillCurrentUserState(dish);
        return dish;
    }

    @Override
    public List<Dish> listByWindow(Long windowId) {
        List<Dish> dishes = list(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getWindowId, windowId)
                .eq(Dish::getStatus, "PUBLISHED")
                .orderByAsc(Dish::getPrice));
        applyStats(dishes);
        return dishes;
    }

    @Override
    public Dish submit(DishDTO dto, Long userId) {
        Window window = windowMapper.selectById(dto.getWindowId());
        if (window == null) throw new BusinessException(404, "所属档口不存在");

        User user = userMapper.selectById(userId);
        String status = "PENDING";
        if (user != null && "ADMIN".equals(user.getRole())) {
            status = StrUtil.blankToDefault(dto.getStatus(), "PUBLISHED");
        }

        Dish dish = new Dish();
        dish.setWindowId(dto.getWindowId());
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setImage(dto.getImage());
        dish.setPrice(dto.getPrice());
        dish.setStatus(status);
        dish.setViewCount(0);
        save(dish);
        return dish;
    }

    @Override
    public void approve(Long id, String status) {
        Dish dish = getById(id);
        if (dish == null) throw new BusinessException(404, "菜品不存在");
        if (!"PUBLISHED".equals(status) && !"PENDING".equals(status)) {
            throw new BusinessException("非法状态");
        }
        Dish upd = new Dish();
        upd.setId(id);
        upd.setStatus(status);
        updateById(upd);
    }

    @Override
    @Transactional
    public void deleteDish(Long id) {
        removeById(id);
    }

    @Override
    public Page<Dish> adminList(String status, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) wrapper.eq(Dish::getStatus, status);
        if (StrUtil.isNotBlank(keyword)) wrapper.like(Dish::getName, keyword);
        wrapper.orderByDesc(Dish::getCreatedAt);
        Page<Dish> result = page(new Page<>(page, size), wrapper);

        Map<Long, Window> windows = windowMapper.selectList(null).stream()
                .collect(Collectors.toMap(Window::getId, Function.identity()));
        result.getRecords().forEach(d -> {
            Window w = windows.get(d.getWindowId());
            if (w != null) d.setWindowName(w.getName());
        });
        return result;
    }

    // ─── 私有方法 ───

    private void applyStats(List<Dish> dishes) {
        if (dishes.isEmpty()) return;

        Map<Long, Map<String, Object>> agg = ratingService.aggregateByTarget("DISH");
        Map<Long, Long> likes = likeService.countByTargets("DISH");
        Map<Long, Window> windows = windowMapper.selectList(null).stream()
                .collect(Collectors.toMap(Window::getId, Function.identity()));

        for (Dish d : dishes) {
            Map<String, Object> stats = agg.get(d.getId());
            if (stats != null) {
                d.setScoreAvg((Double) stats.get("scoreAvg"));
                d.setTasteAvg((Double) stats.get("tasteAvg"));
                d.setValueAvg((Double) stats.get("valueAvg"));
                d.setPortionAvg((Double) stats.get("portionAvg"));
                d.setRatingCount((Long) stats.get("ratingCount"));
                d.setRecentCount((Long) stats.get("recentCount"));
            } else {
                d.setScoreAvg(0.0);
                d.setTasteAvg(0.0);
                d.setValueAvg(0.0);
                d.setPortionAvg(0.0);
                d.setRatingCount(0L);
                d.setRecentCount(0L);
            }
            d.setLikeCount(likes.getOrDefault(d.getId(), 0L));
            Window w = windows.get(d.getWindowId());
            if (w != null) {
                d.setWindow(w);
                d.setWindowName(w.getName());
            }
        }
    }

    private void sort(List<Dish> list, String type) {
        Comparator<Dish> cmp;
        switch (StrUtil.blankToDefault(type, "overall")) {
            case "taste" -> cmp = Comparator
                    .comparingDouble((Dish d) -> d.getTasteAvg() == null ? 0 : d.getTasteAvg())
                    .thenComparingLong(d -> d.getRatingCount() == null ? 0 : d.getRatingCount())
                    .reversed();
            case "hot" -> cmp = Comparator.comparingDouble(this::heat).reversed();
            case "recent" -> cmp = Comparator
                    .comparingLong((Dish d) -> d.getRecentCount() == null ? 0 : d.getRecentCount())
                    .thenComparingDouble(d -> d.getScoreAvg() == null ? 0 : d.getScoreAvg())
                    .reversed();
            default -> cmp = Comparator
                    .comparingDouble((Dish d) -> d.getScoreAvg() == null ? 0 : d.getScoreAvg())
                    .thenComparingLong(d -> d.getRatingCount() == null ? 0 : d.getRatingCount())
                    .reversed();
        }
        list.sort(cmp);
    }

    private double heat(Dish d) {
        long ratingCount = d.getRatingCount() == null ? 0 : d.getRatingCount();
        long likeCount = d.getLikeCount() == null ? 0 : d.getLikeCount();
        int viewCount = d.getViewCount() == null ? 0 : d.getViewCount();
        return ratingCount * 10 + likeCount * 5 + viewCount;
    }

    private void fillCurrentUserState(Dish dish) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId != null) {
            dish.setIsLiked(likeService.isLiked(userId, "DISH", dish.getId()));
            dish.setIsFavorited(favoriteService.isFavorited(userId, "DISH", dish.getId()));
        }
    }
}
