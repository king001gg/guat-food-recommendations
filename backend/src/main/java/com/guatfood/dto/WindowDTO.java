package com.guatfood.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class WindowDTO {

    @NotNull(message = "请选择所属食堂")
    private Long canteenId;

    @NotBlank(message = "档口名称不能为空")
    @Size(max = 50, message = "档口名称最多50字")
    private String name;

    @Size(max = 500, message = "简介最多500字")
    private String description;

    private String coverImage;

    @Size(max = 50, message = "位置最多50字")
    private String location;

    private String status;

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
}
