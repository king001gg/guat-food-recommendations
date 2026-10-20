package com.guatfood.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.guatfood.entity.AuditLog;

import java.time.LocalDateTime;

public interface AuditLogService extends IService<AuditLog> {

    void record(AuditLog log);

    Page<AuditLog> getPage(Integer page, Integer size, String action, Long actorId,
                           Boolean success, LocalDateTime start, LocalDateTime end);

    int purgeBefore(LocalDateTime cutoff);
}
