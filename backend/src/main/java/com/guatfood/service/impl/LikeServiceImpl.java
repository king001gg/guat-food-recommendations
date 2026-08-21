package com.guatfood.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.entity.Dish;
import com.guatfood.entity.Like;
import com.guatfood.entity.Window;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.LikeMapper;
import com.guatfood.mapper.WindowMapper;
import com.guatfood.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Autowired
    private WindowMapper windowMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public boolean toggle(Long userId, String targetType, Long targetId) {
        validateTarget(targetType, targetId);
        Like exist = getOne(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));
        if (exist != null) {
            removeById(exist.getId());
            return false;
        }
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        save(like);
        return true;
    }

    @Override
    public boolean isLiked(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        return count(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)) > 0;
    }

    @Override
    public long getCount(String targetType, Long targetId) {
        return count(new LambdaQueryWrapper<Like>()
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));
    }

    @Override
    public Map<Long, Long> countByTargets(String targetType) {
        List<Like> all = list(new LambdaQueryWrapper<Like>()
                .eq(Like::getTargetType, targetType));
        return all.stream().collect(Collectors.groupingBy(Like::getTargetId, Collectors.counting()));
    }

    @Override
    public Page<Like> myLikes(Long userId, Integer page, Integer size) {
        Page<Like> result = page(new Page<>(page, size),
                new LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId)
                        .orderByDesc(Like::getCreatedAt));
        fillTarget(result.getRecords());
        return result;
    }

    private void validateTarget(String targetType, Long targetId) {
        if ("WINDOW".equals(targetType)) {
            if (windowMapper.selectById(targetId) == null) throw new BusinessException(404, "目标不存在");
        } else if ("DISH".equals(targetType)) {
            if (dishMapper.selectById(targetId) == null) throw new BusinessException(404, "目标不存在");
        } else {
            throw new BusinessException("非法的目标类型");
        }
    }

    private void fillTarget(List<Like> likes) {
        for (Like like : likes) {
            if ("WINDOW".equals(like.getTargetType())) {
                Window w = windowMapper.selectById(like.getTargetId());
                if (w != null) {
                    like.setTargetName(w.getName());
                    like.setTargetImage(w.getCoverImage());
                }
            } else if ("DISH".equals(like.getTargetType())) {
                Dish d = dishMapper.selectById(like.getTargetId());
                if (d != null) {
                    like.setTargetName(d.getName());
                    like.setTargetImage(d.getImage());
                }
            }
        }
    }
}
