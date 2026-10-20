package com.guatfood.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员重置用户密码 */
public class ResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "新密码长度为8-100位")
    private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
