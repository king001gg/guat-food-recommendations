package com.guatfood.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guatfood.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
