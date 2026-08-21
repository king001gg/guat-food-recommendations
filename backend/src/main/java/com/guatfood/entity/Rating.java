package com.guatfood.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 多维评分 (口味/性价比/分量)
 */
@TableName("rating")
public class Rating {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private Integer taste;
    private Integer valueScore;
    private Integer portion;
    private String comment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // ─── 非表字段 ───
    @TableField(exist = false)
    private User user;
    @TableField(exist = false)
    private String targetName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Integer getTaste() { return taste; }
    public void setTaste(Integer taste) { this.taste = taste; }

    public Integer getValueScore() { return valueScore; }
    public void setValueScore(Integer valueScore) { this.valueScore = valueScore; }

    public Integer getPortion() { return portion; }
    public void setPortion(Integer portion) { this.portion = portion; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
