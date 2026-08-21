package com.guatfood.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RatingDTO {

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotNull(message = "口味评分不能为空")
    @Min(value = 1, message = "口味评分1-5星")
    @Max(value = 5, message = "口味评分1-5星")
    private Integer taste;

    @NotNull(message = "性价比评分不能为空")
    @Min(value = 1, message = "性价比评分1-5星")
    @Max(value = 5, message = "性价比评分1-5星")
    private Integer valueScore;

    @NotNull(message = "分量评分不能为空")
    @Min(value = 1, message = "分量评分1-5星")
    @Max(value = 5, message = "分量评分1-5星")
    private Integer portion;

    @Size(max = 500, message = "评语最多500字")
    private String comment;

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
}
