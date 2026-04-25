package com.redmoon2333.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.TaskAssignmentVO;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.entity.Task;
import com.redmoon2333.entity.TaskAssignment;
import com.redmoon2333.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            if (dateTimeStr.endsWith("Z")) {
                OffsetDateTime odt = OffsetDateTime.parse(dateTimeStr, ISO_OFFSET_DATE_TIME_FORMATTER);
                return odt.toLocalDateTime();
            }
            return LocalDateTime.parse(dateTimeStr, ISO_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无效的日期格式: " + dateTimeStr);
        }
    }

    private final TaskService taskService;

    @PostMapping
    @RequireMinisterRole
    public ApiResponse<Task> createTask(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Integer creatorId = (Integer) request.getAttribute("userId");
        String title = (String) body.get("title");
        String description = (String) body.get("description");

        if (title == null || title.isBlank()) {
            return ApiResponse.error("任务标题不能为空", ErrorCode.INVALID_REQUEST_PARAMETER.getCode());
        }

        // Bug 2 fix: dueTime 为空字符串时 LocalDateTime.parse("") 会抛异常
        LocalDateTime dueTime = null;
        Object dueTimeObj = body.get("dueTime");
        if (dueTimeObj != null) {
            String dueTimeStr = dueTimeObj.toString();
            if (!dueTimeStr.isBlank()) {
                dueTime = parseDateTime(dueTimeStr);
            }
        }

        // Bug 7 fix: assigneeIds 可能为 null，直接强转会 NPE
        Integer priority = 1;
        Object priorityObj = body.get("priority");
        if (priorityObj != null) {
            priority = ((Number) priorityObj).intValue();
        }

        Object assigneeIdsObj = body.get("assigneeIds");
        List<Integer> assigneeIds = assigneeIdsObj != null
                ? ((List<?>) assigneeIdsObj).stream()
                    .filter(item -> item instanceof Number)
                    .map(item -> ((Number) item).intValue())
                    .toList()
                : List.of();

        // 固定24小时冷却，不接受前端传入
        final int REMIND_COOLDOWN_MINUTES = 24 * 60;

        Task task = taskService.createTask(creatorId, title, description, dueTime, priority, assigneeIds, REMIND_COOLDOWN_MINUTES);
        return ApiResponse.success(task);
    }

    @GetMapping("/mine")
    @RequireMemberRole
    public ApiResponse<IPage<TaskAssignmentVO>> getMyTasks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Integer userId = (Integer) request.getAttribute("userId");
        IPage<TaskAssignmentVO> result = taskService.getMyTasks(userId, page, pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping("/created")
    @RequireMinisterRole
    public ApiResponse<IPage<Task>> getCreatedTasks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        Integer userId = (Integer) request.getAttribute("userId");
        IPage<Task> result = taskService.getCreatedTasks(userId, page, pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @RequireMemberRole
    public ApiResponse<Map<String, Object>> getTaskDetail(@PathVariable Long id) {
        Map<String, Object> detail = taskService.getTaskDetail(id);
        return ApiResponse.success(detail);
    }

    @PostMapping("/assignments/{assignmentId}/done")
    @RequireMemberRole
    public ApiResponse<Void> markDone(
            HttpServletRequest request,
            @PathVariable Long assignmentId,
            @RequestBody(required = false) Map<String, String> body) {
        Integer userId = (Integer) request.getAttribute("userId");
        String remark = body != null ? body.get("doneRemark") : null;
        taskService.markDone(assignmentId, userId, remark);
        return ApiResponse.success(null);
    }

    @PostMapping("/assignments/{assignmentId}/remind")
    @RequireMinisterRole
    public ApiResponse<Void> remind(
            HttpServletRequest request,
            @PathVariable Long assignmentId) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        taskService.remind(assignmentId, operatorId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    @RequireMinisterRole
    public ApiResponse<Task> updateTask(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        String title = (String) body.get("title");
        String description = (String) body.get("description");

        // Bug 8 fix: 同 Bug 2，dueTime 为空字符串时 parse("") 会炸
        LocalDateTime dueTime = null;
        Object dueTimeObj = body.get("dueTime");
        if (dueTimeObj != null) {
            String dueTimeStr = dueTimeObj.toString();
            if (!dueTimeStr.isBlank()) {
                dueTime = parseDateTime(dueTimeStr);
            }
        }

        Integer priority = null;
        Object priorityObj = body.get("priority");
        if (priorityObj != null) {
            priority = ((Number) priorityObj).intValue();
        }

        // 固定24小时冷却，不接受前端传入
        final int REMIND_COOLDOWN_MINUTES = 24 * 60;

        Task task = taskService.updateTask(id, operatorId, title, description, dueTime, priority, REMIND_COOLDOWN_MINUTES);
        return ApiResponse.success(task);
    }

    @DeleteMapping("/{id}")
    @RequireMinisterRole
    public ApiResponse<Void> deleteTask(
            HttpServletRequest request,
            @PathVariable Long id) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        taskService.deleteTask(id, operatorId);
        return ApiResponse.success(null);
    }

    @GetMapping("/candidates")
    @RequireMinisterRole
    public ApiResponse<List<Map<String, Object>>> getCandidates(HttpServletRequest request) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        List<Map<String, Object>> candidates = taskService.getCandidates(operatorId);
        return ApiResponse.success(candidates);
    }
}
