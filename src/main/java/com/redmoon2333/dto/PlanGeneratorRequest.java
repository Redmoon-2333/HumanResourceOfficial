package com.redmoon2333.dto;

/**
 * 策划案生成请求DTO
 */
public class PlanGeneratorRequest {
    private String theme;           // 活动主题/名称
    private String organizer;       // 主办单位
    private String eventTime;       // 活动时间
    private String eventLocation;   // 活动地点
    private String staff;           // 工作人员
    private String participants;    // 参与人员
    private String purpose;         // 活动目的
    private Integer leaderCount;    // 部长/副部长数量
    private Integer memberCount;    // 部员数量
    
    public PlanGeneratorRequest() {}
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public String getOrganizer() {
        return organizer;
    }
    
    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }
    
    public String getEventTime() {
        return eventTime;
    }
    
    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }
    
    public String getEventLocation() {
        return eventLocation;
    }
    
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }
    
    public String getStaff() {
        return staff;
    }
    
    public void setStaff(String staff) {
        this.staff = staff;
    }
    
    public String getParticipants() {
        return participants;
    }
    
    public void setParticipants(String participants) {
        this.participants = participants;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public Integer getLeaderCount() {
        return leaderCount;
    }
    
    public void setLeaderCount(Integer leaderCount) {
        this.leaderCount = leaderCount;
    }
    
    public Integer getMemberCount() {
        return memberCount;
    }
    
    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}
