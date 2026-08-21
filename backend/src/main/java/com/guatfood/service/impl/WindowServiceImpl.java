package com.guatfood.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.common.PageResult;
import com.guatfood.common.PageUtil;
import com.guatfood.config.JwtInterceptor;
import com.guatfood.dto.WindowDTO;
import com.guatfood.entity.Canteen;
import com.guatfood.entity.Dish;
import com.guatfood.entity.User;
import com.guatfood.entity.Window;
import com.guatfood.mapper.CanteenMapper;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.UserMapper;
import com.guatfood.mapper.WindowMapper;
import com.guatfood.service.FavoriteService;
import com.guatfood.service.LikeService;
import com.guatfood.service.RatingService;
import com.guatfood.service.WindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WindowServiceImpl extends ServiceImpl<WindowMapper, Window> implements WindowService {

    @Autowired
    private CanteenMapper canteenMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FavoriteService favoriteService;

    @Override
    public PageResult<Window> rank(String type, Long canteenId, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Window> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Window::getStatus, "PUBLISHED");
        if (canteenId != null) wrapper.eq(Window::getCanteenId, canteenId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Window::getName, keyword).or().like(Window::getDescription, keyword));
        }
        List<Window> all = list(wrapper);
        applyStats(all);
        sort(all, type);
        return PageUtil.paginate(all, page, size);
    }

    @Override
    public Window getDetail(Long id) {
        Window window = getById(id);
        if (window == null) throw new BusinessException(404, "档口不存在");

        // 未发布的档口仅管理员可见（防止绕过审核直接访问）
        if (!"PUBLISHED".equals(window.getStatus())) {
            Long uid = JwtInterceptor.getCurrentUserId();
            User u = uid == null ? null : userMapper.selectById(uid);
            if (u == null || !"ADMIN".equals(u.getRole())) {
                throw new BusinessException(404, "档口不存在或未发布");
            }
        }

        // 浏览量 +1
        Window upd = new Window();
        upd.setId(id);
        upd.setViewCount((window.getViewCount() == null ? 0 : window.getViewCount()) + 1);
        updateById(upd);
        window.setViewCount(upd.getViewCount());

        applyStats(List.of(window));
        fillCurrentUserState(window);
        return window;
    }

    @Override
    public Window submit(WindowDTO dto, Long userId) {
        User user = userMapper.selectById(userId);
        String status = "PENDING";
        if (user != null && "ADMIN".equals(user.getRole())) {
            status = StrUtil.blankToDefault(dto.getStatus(), "PUBLISHED");
        }

        Window window = new Window();
        window.setCanteenId(dto.getCanteenId());
        window.setName(dto.getName());
        window.setDescription(dto.getDescription());
        window.setCoverImage(dto.getCoverImage());
        window.setLocation(dto.getLocation());
        window.setStatus(status);
        window.setViewCount(0);
        save(window);
        return window;
    }

    @Override
    public void approve(Long id, String status) {
        Window window = getById(id);
        if (window == null) throw new BusinessException(404, "档口不存在");
        if (!"PUBLISHED".equals(status) && !"PENDING".equals(status)) {
            throw new BusinessException("非法状态");
        }
        Window upd = new Window();
        upd.setId(id);
        upd.setStatus(status);
        updateById(upd);
    }

    @Override
    @Transactional
    public void deleteWindow(Long id) {
        removeById(id);
        // 同时删除旗下菜品
        dishMapper.delete(new LambdaQueryWrapper<Dish>().eq(Dish::getWindowId, id));
    }

    @Override
    public Page<Window> adminList(String status, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Window> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) wrapper.eq(Window::getStatus, status);
        if (StrUtil.isNotBlank(keyword)) wrapper.like(Window::getName, keyword);
        wrapper.orderByDesc(Window::getCreatedAt);
        Page<Window> result = page(new Page<>(page, size), wrapper);

        Map<Long, Canteen> canteens = canteenMapper.selectList(null).stream()
                .collect(Collectors.toMap(Canteen::getId, Function.identity()));
        result.getRecords().forEach(w -> w.setCanteen(canteens.get(w.getCanteenId())));
        return result;
    }

    // ─── 私有方法 ───

    private void applyStats(List<Window> windows) {
        if (windows.isEmpty()) return;

        Map<Long, Map<String, Object>> agg = ratingService.aggregateByTarget("WINDOW");
        Map<Long, Long> likes = likeService.countByTargets("WINDOW");
        Map<Long, Long> dishCounts = dishMapper.selectList(
                        new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, "PUBLISHED")).stream()
                .collect(Collectors.groupingBy(Dish::getWindowId, Collectors.counting()));
        Map<Long, Canteen> canteens = canteenMapper.selectList(null).stream()
                .collect(Collectors.toMap(Canteen::getId, Function.identity()));

        for (Window w : windows) {
            Map<String, Object> stats = agg.get(w.getId());
            if (stats != null) {
                w.setScoreAvg((Double) stats.get("scoreAvg"));
                w.setTasteAvg((Double) stats.get("tasteAvg"));
                w.setValueAvg((Double) stats.get("valueAvg"));
                w.setPortionAvg((Double) stats.get("portionAvg"));
                w.setRatingCount((Long) stats.get("ratingCount"));
                w.setRecentCount((Long) stats.get("recentCount"));
            } else {
                w.setScoreAvg(0.0);
                w.setTasteAvg(0.0);
                w.setValueAvg(0.0);
                w.setPortionAvg(0.0);
                w.setRatingCount(0L);
                w.setRecentCount(0L);
            }
            w.setLikeCount(likes.getOrDefault(w.getId(), 0L));
            w.setDishCount(dishCounts.getOrDefault(w.getId(), 0L));
            w.setCanteen(canteens.get(w.getCanteenId()));
        }
    }

    private void sort(List<Window> list, String type) {
        Comparator<Window> cmp;
        switch (StrUtil.blankToDefault(type, "overall")) {
            case "taste" -> cmp = Comparator
                    .comparingDouble((Window w) -> w.getTasteAvg() == null ? 0 : w.getTasteAvg())
                    .thenComparingLong(w -> w.getRatingCount() == null ? 0 : w.getRatingCount())
                    .reversed();
            case "hot" -> cmp = Comparator
                    .comparingDouble(this::heat).reversed();
            case "recent" -> cmp = Comparator
                    .comparingLong((Window w) -> w.getRecentCount() == null ? 0 : w.getRecentCount())
                    .thenComparingDouble(w -> w.getScoreAvg() == null ? 0 : w.getScoreAvg())
                    .reversed();
            default -> cmp = Comparator
                    .comparingDouble((Window w) -> w.getScoreAvg() == null ? 0 : w.getScoreAvg())
                    .thenComparingLong(w -> w.getRatingCount() == null ? 0 : w.getRatingCount())
                    .reversed();
        }
        list.sort(cmp);
    }

    private double heat(Window w) {
        long ratingCount = w.getRatingCount() == null ? 0 : w.getRatingCount();
        long likeCount = w.getLikeCount() == null ? 0 : w.getLikeCount();
        int viewCount = w.getViewCount() == null ? 0 : w.getViewCount();
        return ratingCount * 10 + likeCount * 5 + viewCount;
    }

    private void fillCurrentUserState(Window window) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId != null) {
            window.setIsLiked(likeService.isLiked(userId, "WINDOW", window.getId()));
            window.setIsFavorited(favoriteService.isFavorited(userId, "WINDOW", window.getId()));
        }
    }
}
