package com.guatfood.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.dto.RatingDTO;
import com.guatfood.entity.Dish;
import com.guatfood.entity.Rating;
import com.guatfood.entity.User;
import com.guatfood.entity.Window;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.RatingMapper;
import com.guatfood.mapper.UserMapper;
import com.guatfood.mapper.WindowMapper;
import com.guatfood.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RatingServiceImpl extends ServiceImpl<RatingMapper, Rating> implements RatingService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WindowMapper windowMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public Rating submit(RatingDTO dto, Long userId) {
        validateTarget(dto.getTargetType(), dto.getTargetId());

        Rating exist = getOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getTargetType, dto.getTargetType())
                .eq(Rating::getTargetId, dto.getTargetId()));

        Rating rating = exist != null ? exist : new Rating();
        rating.setUserId(userId);
        rating.setTargetType(dto.getTargetType());
        rating.setTargetId(dto.getTargetId());
        rating.setTaste(dto.getTaste());
        rating.setValueScore(dto.getValueScore());
        rating.setPortion(dto.getPortion());
        rating.setComment(dto.getComment());

        if (exist != null) {
            updateById(rating);
        } else {
            save(rating);
        }
        return rating;
    }

    @Override
    public Page<Rating> listByTarget(String targetType, Long targetId, Integer page, Integer size) {
        Page<Rating> result = page(new Page<>(page, size),
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getTargetType, targetType)
                        .eq(Rating::getTargetId, targetId)
                        .orderByDesc(Rating::getCreatedAt));
        fillUserAndTarget(result.getRecords());
        return result;
    }

    @Override
    public Page<Rating> myRatings(Long userId, Integer page, Integer size) {
        Page<Rating> result = page(new Page<>(page, size),
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getUserId, userId)
                        .orderByDesc(Rating::getCreatedAt));
        fillUserAndTarget(result.getRecords());
        return result;
    }

    @Override
    public Page<Rating> adminList(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<Rating> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Rating::getComment, keyword);
        }
        wrapper.orderByDesc(Rating::getCreatedAt);
        Page<Rating> result = page(new Page<>(page, size), wrapper);
        fillUserAndTarget(result.getRecords());
        return result;
    }

    @Override
    public void deleteMine(Long id, Long userId) {
        Rating rating = getById(id);
        if (rating == null) throw new BusinessException(404, "评分不存在");
        if (!rating.getUserId().equals(userId)) throw new BusinessException(403, "只能删除自己的评分");
        removeById(id);
    }

    @Override
    public void adminDelete(Long id) {
        removeById(id);
    }

    @Override
    public Map<Long, Map<String, Object>> aggregateByTarget(String targetType) {
        List<Rating> ratings = list(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getTargetType, targetType));
        Map<Long, List<Rating>> grouped = ratings.stream()
                .collect(Collectors.groupingBy(Rating::getTargetId));

        LocalDateTime recent = LocalDateTime.now().minusDays(30);
        Map<Long, Map<String, Object>> result = new HashMap<>();
        grouped.forEach((targetId, list) -> {
            double taste = list.stream().mapToInt(Rating::getTaste).average().orElse(0);
            double value = list.stream().mapToInt(Rating::getValueScore).average().orElse(0);
            double portion = list.stream().mapToInt(Rating::getPortion).average().orElse(0);
            long recentCount = list.stream()
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(recent))
                    .count();

            Map<String, Object> m = new HashMap<>();
            m.put("ratingCount", (long) list.size());
            m.put("tasteAvg", round1(taste));
            m.put("valueAvg", round1(value));
            m.put("portionAvg", round1(portion));
            m.put("scoreAvg", round1((taste + value + portion) / 3.0));
            m.put("recentCount", recentCount);
            result.put(targetId, m);
        });
        return result;
    }

    private void validateTarget(String targetType, Long targetId) {
        if ("WINDOW".equals(targetType)) {
            Window w = windowMapper.selectById(targetId);
            if (w == null) throw new BusinessException(404, "档口不存在");
        } else if ("DISH".equals(targetType)) {
            Dish d = dishMapper.selectById(targetId);
            if (d == null) throw new BusinessException(404, "菜品不存在");
        } else {
            throw new BusinessException("非法的目标类型");
        }
    }

    private void fillUserAndTarget(List<Rating> ratings) {
        for (Rating r : ratings) {
            User u = userMapper.selectById(r.getUserId());
            if (u != null) {
                u.setPassword(null);
                r.setUser(u);
            }
            r.setTargetName(resolveTargetName(r.getTargetType(), r.getTargetId()));
        }
    }

    private String resolveTargetName(String targetType, Long targetId) {
        if ("WINDOW".equals(targetType)) {
            Window w = windowMapper.selectById(targetId);
            return w != null ? w.getName() : null;
        } else if ("DISH".equals(targetType)) {
            Dish d = dishMapper.selectById(targetId);
            return d != null ? d.getName() : null;
        }
        return null;
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
