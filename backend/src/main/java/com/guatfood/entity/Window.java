package com.guatfood.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 档口/窗口
 */
@TableName("food_window")
public class Window {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long canteenId;
    private String name;
    private String description;
    private String coverImage;
    private String location;
    private String status;
    private Integer viewCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // ─── 非表字段: 关联与统计 ───
    @TableField(exist = false)
    private Canteen canteen;
    @TableField(exist = false)
    private Double scoreAvg;
    @TableField(exist = false)
    private Double tasteAvg;
    @TableField(exist = false)
    private Double valueAvg;
    @TableField(exist = false)
    private Double portionAvg;
    @TableField(exist = false)
    private Long ratingCount;
    @TableField(exist = false)
    private Long recentCount;
    @TableField(exist = false)
    private Long likeCount;
    @TableField(exist = false)
    private Long dishCount;
    @TableField(exist = false)
    private Boolean isLiked;
    @TableField(exist = false)
    private Boolean isFavorited;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCanteenId() { return canteenId; }
    public void setCanteenId(Long canteenId) { this.canteenId = canteenId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Canteen getCanteen() { return canteen; }
    public void setCanteen(Canteen canteen) { this.canteen = canteen; }

    public Double getScoreAvg() { return scoreAvg; }
    public void setScoreAvg(Double scoreAvg) { this.scoreAvg = scoreAvg; }

    public Double getTasteAvg() { return tasteAvg; }
    public void setTasteAvg(Double tasteAvg) { this.tasteAvg = tasteAvg; }

    public Double getValueAvg() { return valueAvg; }
    public void setValueAvg(Double valueAvg) { this.valueAvg = valueAvg; }

    public Double getPortionAvg() { return portionAvg; }
    public void setPortionAvg(Double portionAvg) { this.portionAvg = portionAvg; }

    public Long getRatingCount() { return ratingCount; }
    public void setRatingCount(Long ratingCount) { this.ratingCount = ratingCount; }

    public Long getRecentCount() { return recentCount; }
    public void setRecentCount(Long recentCount) { this.recentCount = recentCount; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Long getDishCount() { return dishCount; }
    public void setDishCount(Long dishCount) { this.dishCount = dishCount; }

    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }

    public Boolean getIsFavorited() { return isFavorited; }
    public void setIsFavorited(Boolean isFavorited) { this.isFavorited = isFavorited; }
}
