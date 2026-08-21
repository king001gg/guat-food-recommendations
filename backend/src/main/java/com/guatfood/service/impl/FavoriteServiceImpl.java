package com.guatfood.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.entity.Dish;
import com.guatfood.entity.Favorite;
import com.guatfood.entity.Window;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.FavoriteMapper;
import com.guatfood.mapper.WindowMapper;
import com.guatfood.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Autowired
    private WindowMapper windowMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public boolean toggle(Long userId, String targetType, Long targetId) {
        validateTarget(targetType, targetId);
        Favorite exist = getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));
        if (exist != null) {
            removeById(exist.getId());
            return false;
        }
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setTargetType(targetType);
        fav.setTargetId(targetId);
        save(fav);
        return true;
    }

    @Override
    public boolean isFavorited(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        return count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId)) > 0;
    }

    @Override
    public Map<Long, Long> countByTargets(String targetType) {
        List<Favorite> all = list(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getTargetType, targetType));
        return all.stream().collect(Collectors.groupingBy(Favorite::getTargetId, Collectors.counting()));
    }

    @Override
    public Page<Favorite> myFavorites(Long userId, Integer page, Integer size) {
        Page<Favorite> result = page(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt));
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

    private void fillTarget(List<Favorite> favorites) {
        for (Favorite fav : favorites) {
            if ("WINDOW".equals(fav.getTargetType())) {
                Window w = windowMapper.selectById(fav.getTargetId());
                if (w != null) {
                    fav.setTargetName(w.getName());
                    fav.setTargetImage(w.getCoverImage());
                }
            } else if ("DISH".equals(fav.getTargetType())) {
                Dish d = dishMapper.selectById(fav.getTargetId());
                if (d != null) {
                    fav.setTargetName(d.getName());
                    fav.setTargetImage(d.getImage());
                }
            }
        }
    }
}
