package com.redmoon2333.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    @Column(name = "username")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    @Column(name = "password")
    private String password;
    
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    @Column(name = "name")
    private String name;
    
    @Column(name = "role_history")
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
    
    // 带姓名的构造函数
    public User(String username, String password, String name) {
        this(username, password);
        this.name = name;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
                ", name='" + name + '\'' +
                ", roleHistory='" + roleHistory + '\'' +
                '}';
    }
}