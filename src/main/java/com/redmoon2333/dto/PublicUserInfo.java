package com.redmoon2333.dto;

public class PublicUserInfo {
    private String name;
    private String roleHistory;

    public PublicUserInfo() {}

    public PublicUserInfo(String name, String roleHistory) {
        this.name = name;
        this.roleHistory = roleHistory;
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
}