package com.redmoon2333.dto;

import com.redmoon2333.validation.ValidRoleHistory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
    
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    @NotBlank(message = "激活码不能为空")
    private String activationCode;
    
    @ValidRoleHistory
    @NotBlank(message = "角色历史不能为空")
    private String roleHistory;
    
    // 无参构造函数
    public RegisterRequest() {}
    
    // 带参数的构造函数
    public RegisterRequest(String username, String password, String confirmPassword, String activationCode) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.activationCode = activationCode;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getActivationCode() {
        return activationCode;
    }
    
    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }
    
    public String getRoleHistory() {
        return roleHistory;
    }
    
    public void setRoleHistory(String roleHistory) {
        this.roleHistory = roleHistory;
    }
}