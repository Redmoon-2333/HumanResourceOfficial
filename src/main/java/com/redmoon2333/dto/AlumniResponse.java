package com.redmoon2333.dto;

import java.util.List;

public class AlumniResponse {
    private Integer year;
    private List<AlumniMember> members;
    private Integer uniqueMemberCount;

    public AlumniResponse() {}

    public AlumniResponse(Integer year, List<AlumniMember> members) {
        this.year = year;
        this.members = members;
    }

    public AlumniResponse(Integer year, List<AlumniMember> members, Integer uniqueMemberCount) {
        this.year = year;
        this.members = members;
        this.uniqueMemberCount = uniqueMemberCount;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<AlumniMember> getMembers() {
        return members;
    }

    public void setMembers(List<AlumniMember> members) {
        this.members = members;
    }

    public Integer getUniqueMemberCount() {
        return uniqueMemberCount;
    }

    public void setUniqueMemberCount(Integer uniqueMemberCount) {
        this.uniqueMemberCount = uniqueMemberCount;
    }
}