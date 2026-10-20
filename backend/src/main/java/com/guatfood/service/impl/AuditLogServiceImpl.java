package com.guatfood.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.guatfood.common.BusinessException;
import com.guatfood.entity.AuditLog;
import com.guatfood.mapper.AuditLogMapper;
import com.guatfood.service.AuditLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog>
        implements AuditLogService {

    private static final Log log = LogFactory.get();

    @Value("${app.audit.retention-days:180}")
    private int retentionDays;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog auditLog) {
        save(auditLog);
    }

    @Override
    public Page<AuditLog> getPage(Integer page, Integer size, String action, Long actorId,
                                  Boolean success, LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(400, "开始时间不能晚于结束时间");
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(action)) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (actorId != null) {
            wrapper.eq(AuditLog::getActorId, actorId);
        }
        if (success != null) {
            wrapper.eq(AuditLog::getSuccess, success);
        }
        if (start != null) {
            wrapper.ge(AuditLog::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(AuditLog::getCreatedAt, end);
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return page(new Page<>(safePage, safeSize), wrapper);
    }

    @Override
    public int purgeBefore(LocalDateTime cutoff) {
        return baseMapper.delete(new LambdaQueryWrapper<AuditLog>()
                .lt(AuditLog::getCreatedAt, cutoff));
    }

    /** 每天凌晨清理过期审计日志，避免表无限增长 */
    @Scheduled(cron = "${app.audit.cleanup-cron:0 30 3 * * ?}")
    public void cleanup() {
        if (retentionDays <= 0) {
            return;
        }
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            int deleted = purgeBefore(cutoff);
            if (deleted > 0) {
                log.info("审计日志清理完成：删除 {} 条，保留最近 {} 天", deleted, retentionDays);
            }
        } catch (Exception e) {
            log.warn("审计日志清理失败: {}", e.getMessage());
        }
    }
}
