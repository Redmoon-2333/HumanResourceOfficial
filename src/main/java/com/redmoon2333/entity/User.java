package com.redmoon2333.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
@Entity
@Table(name = "user")
public class User {
    @Id
    private Integer userId;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
    
    private String roleHistory;
    
    // 无参构造函数
    public User() {
    }
    
    // 带参数的构造函数
    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
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
    
    public String getRoleHistory() {
        return roleHistory;
    }
    
    public void setRoleHistory(String roleHistory) {
        this.roleHistory = roleHistory;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", roleHistory='" + roleHistory + '\'' +
                '}';
    }
}