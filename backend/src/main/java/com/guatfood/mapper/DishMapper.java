package com.guatfood.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guatfood.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
