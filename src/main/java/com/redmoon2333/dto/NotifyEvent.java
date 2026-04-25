package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String eventId;
    private NotifyType type;
    private Integer receiverId;
    private Integer senderId;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    private Map<String, Object> extra;
    private LocalDateTime eventTime;
}
