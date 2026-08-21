package com.guatfood.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guatfood.entity.Canteen;
import com.guatfood.mapper.CanteenMapper;
import com.guatfood.service.CanteenService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CanteenServiceImpl extends ServiceImpl<CanteenMapper, Canteen> implements CanteenService {

    @Override
    public List<Canteen> listAll() {
        return list(new LambdaQueryWrapper<Canteen>().orderByAsc(Canteen::getSortOrder));
    }

    @Override
    public Canteen create(Canteen canteen) {
        if (canteen.getSortOrder() == null) canteen.setSortOrder(0);
        save(canteen);
        return canteen;
    }

    @Override
    public Canteen update(Canteen canteen) {
        updateById(canteen);
        return getById(canteen.getId());
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }
}
