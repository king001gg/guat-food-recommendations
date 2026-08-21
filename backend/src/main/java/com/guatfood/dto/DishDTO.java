package com.guatfood.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class DishDTO {

    @NotNull(message = "请选择所属档口")
    private Long windowId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 50, message = "菜品名称最多50字")
    private String name;

    @Size(max = 500, message = "简介最多500字")
    private String description;

    private String image;

    private BigDecimal price;

    private String status;

    public Long getWindowId() { return windowId; }
    public void setWindowId(Long windowId) { this.windowId = windowId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
