package com.guatfood.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guatfood.entity.Canteen;

import java.util.List;

public interface CanteenService extends IService<Canteen> {

    List<Canteen> listAll();

    Canteen create(Canteen canteen);

    Canteen update(Canteen canteen);

    void delete(Long id);
}
