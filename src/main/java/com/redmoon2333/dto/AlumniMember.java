package com.redmoon2333.dto;

public class AlumniMember {
    private String name;
    private String role; // 部员、部长、副部长等职位

    public AlumniMember() {}

    public AlumniMember(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}