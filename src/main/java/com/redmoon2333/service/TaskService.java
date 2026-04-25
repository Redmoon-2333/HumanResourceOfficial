package com.redmoon2333.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.redmoon2333.dto.NotifyEvent;
import com.redmoon2333.dto.NotifyType;
import com.redmoon2333.dto.TaskAssignmentVO;
import com.redmoon2333.entity.Task;
import com.redmoon2333.entity.TaskAssignment;
import com.redmoon2333.entity.TaskRemindLog;
import com.redmoon2333.entity.User;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.TaskAssignmentMapper;
import com.redmoon2333.mapper.TaskMapper;
import com.redmoon2333.mapper.TaskRemindLogMapper;
import com.redmoon2333.mapper.UserMapper;
import com.redmoon2333.mq.producer.NotifyProducer;
import com.redmoon2333.util.RoleHistoryParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TaskRemindLogMapper taskRemindLogMapper;
    private final UserMapper userMapper;
    private final NotifyProducer notifyProducer;

    @Transactional
    public Task createTask(Integer creatorId, String title, String description,
                           LocalDateTime dueTime, Integer priority,
                           List<Integer> assigneeIds, Integer remindCooldownMinutes) {
        User creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        RoleHistoryParser.ParsedRole creatorRole = RoleHistoryParser.getLatestRole(creator);
        int targetYear = creatorRole.year();

        for (Integer assigneeId : assigneeIds) {
            User assignee = userMapper.selectById(assigneeId);
            if (assignee == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            boolean hasRoleInTargetYear = RoleHistoryParser.parseRoleHistory(assignee.getRoleHistory()).stream()
                    .map(entry -> {
                        try {
                            return RoleHistoryParser.parseSingleRole(entry).year();
                        } catch (BusinessException e) {
                            return null;
                        }
                    })
                    .filter(year -> year != null)
                    .anyMatch(year -> year == targetYear);
            if (!hasRoleInTargetYear) {
                throw new BusinessException(ErrorCode.ASSIGNEE_YEAR_MISMATCH);
            }
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setCreatorId(creatorId);
        task.setCreatorYear(targetYear);
        task.setTargetYear(targetYear);
        task.setDueTime(dueTime);
        task.setPriority(priority != null ? priority : 1);
        task.setStatus("OPEN");
        task.setRemindCooldownMinutes(remindCooldownMinutes != null ? remindCooldownMinutes : 60);
        taskMapper.insert(task);

        for (Integer assigneeId : assigneeIds) {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setTaskId(task.getTaskId());
            assignment.setAssigneeId(assigneeId);
            assignment.setStatus("PENDING");
            taskAssignmentMapper.insert(assignment);
        }

        for (Integer assigneeId : assigneeIds) {
            notifyProducer.send(NotifyEvent.builder()
                    .type(NotifyType.TASK_ASSIGNED)
                    .receiverId(assigneeId)
                    .senderId(creatorId)
                    .title("新任务分配：" + title)
                    .content("部长 " + creator.getName() + " 向你分配了任务「" + title + "」")
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .build());
        }

        log.info("任务创建成功: taskId={}, creatorId={}, assigneeCount={}", task.getTaskId(), creatorId, assigneeIds.size());
        return task;
    }

    public IPage<TaskAssignmentVO> getMyTasks(Integer userId, Integer page, Integer pageSize) {
        Page<TaskAssignment> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskAssignment::getAssigneeId, userId);
        wrapper.orderByDesc(TaskAssignment::getCreateTime);
        IPage<TaskAssignment> pageResult = taskAssignmentMapper.selectPage(pageParam, wrapper);

        List<Long> taskIds = pageResult.getRecords().stream()
                .map(TaskAssignment::getTaskId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Task> taskMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<Task> tasks = taskMapper.selectBatchIds(taskIds);
            taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskId, t -> t));
        }

        Map<Integer, User> creatorMap = new HashMap<>();
        List<Integer> creatorIds = taskMap.values().stream()
                .map(Task::getCreatorId)
                .distinct()
                .collect(Collectors.toList());
        if (!creatorIds.isEmpty()) {
            List<User> creators = userMapper.selectBatchIds(creatorIds);
            creatorMap = creators.stream().collect(Collectors.toMap(User::getUserId, u -> u));
        }

        final Map<Long, Task> finalTaskMap = taskMap;
        final Map<Integer, User> finalCreatorMap = creatorMap;
        List<TaskAssignmentVO> voList = pageResult.getRecords().stream().map(a -> {
            TaskAssignmentVO vo = new TaskAssignmentVO();
            vo.setAssignmentId(a.getAssignmentId());
            vo.setTaskId(a.getTaskId());
            vo.setAssigneeId(a.getAssigneeId());
            vo.setStatus(a.getStatus());
            vo.setDoneTime(a.getDoneTime());
            vo.setDoneRemark(a.getDoneRemark());
            vo.setLastRemindTime(a.getLastRemindTime());
            vo.setRemindCount(a.getRemindCount());
            vo.setCreateTime(a.getCreateTime());

            Task task = finalTaskMap.get(a.getTaskId());
            if (task != null) {
                vo.setTaskTitle(task.getTitle());
                vo.setTaskDescription(task.getDescription());
                vo.setTaskDueTime(task.getDueTime());
                vo.setTaskPriority(task.getPriority());
                vo.setTaskStatus(task.getStatus());
                vo.setCreatorId(task.getCreatorId());
                User creator = finalCreatorMap.get(task.getCreatorId());
                vo.setCreatorName(creator != null ? creator.getName() : null);
            }
            return vo;
        }).collect(Collectors.toList());

        Page<TaskAssignmentVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    public IPage<Task> getCreatedTasks(Integer creatorId, Integer page, Integer pageSize) {
        Page<Task> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getCreatorId, creatorId);
        wrapper.orderByDesc(Task::getCreateTime);
        return taskMapper.selectPage(pageParam, wrapper);
    }

    public Map<String, Object> getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }

        List<TaskAssignment> assignments = taskAssignmentMapper.selectList(
                new LambdaQueryWrapper<TaskAssignment>()
                        .eq(TaskAssignment::getTaskId, taskId));

        // 批量查询：收集所有需要的用户 ID，一次查询
        Set<Integer> neededUserIds = new HashSet<>();
        neededUserIds.add(task.getCreatorId());
        for (TaskAssignment a : assignments) {
            neededUserIds.add(a.getAssigneeId());
        }
        List<User> neededUsers = userMapper.selectBatchIds(neededUserIds);
        Map<Integer, User> userMap = neededUsers.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<Map<String, Object>> assignmentVOs = new ArrayList<>();
        for (TaskAssignment a : assignments) {
            User assignee = userMap.get(a.getAssigneeId());
            Map<String, Object> vo = Map.of(
                    "assignmentId", a.getAssignmentId(),
                    "assigneeId", a.getAssigneeId(),
                    "assigneeName", assignee != null ? assignee.getName() : "未知",
                    "assigneeStudentId", assignee != null && assignee.getStudentId() != null ? assignee.getStudentId() : "",
                    "status", a.getStatus(),
                    "doneTime", a.getDoneTime() != null ? a.getDoneTime().toString() : "",
                    "doneRemark", a.getDoneRemark() != null ? a.getDoneRemark() : "",
                    "lastRemindTime", a.getLastRemindTime() != null ? a.getLastRemindTime().toString() : "",
                    "remindCount", a.getRemindCount()
            );
            assignmentVOs.add(vo);
        }

        User creator = userMap.get(task.getCreatorId());
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("title", task.getTitle());
        result.put("description", task.getDescription() != null ? task.getDescription() : "");
        result.put("creatorId", task.getCreatorId());
        result.put("creatorName", creator != null ? creator.getName() : "未知");
        result.put("creatorYear", task.getCreatorYear());
        result.put("targetYear", task.getTargetYear());
        result.put("dueTime", task.getDueTime() != null ? task.getDueTime().toString() : "");
        result.put("priority", task.getPriority());
        result.put("status", task.getStatus());
        result.put("remindCooldownMinutes", task.getRemindCooldownMinutes());
        result.put("assignments", assignmentVOs);
        result.put("createTime", task.getCreateTime() != null ? task.getCreateTime().toString() : "");
        result.put("updateTime", task.getUpdateTime() != null ? task.getUpdateTime().toString() : "");
        return result;
    }

    @Transactional
    public void markDone(Long assignmentId, Integer userId, String remark) {
        TaskAssignment assignment = taskAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ErrorCode.TASK_ASSIGNMENT_NOT_FOUND);
        }
        if (!assignment.getAssigneeId().equals(userId)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS);
        }
        if ("DONE".equals(assignment.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_DONE);
        }

        assignment.setStatus("DONE");
        assignment.setDoneTime(LocalDateTime.now());
        assignment.setDoneRemark(remark);
        taskAssignmentMapper.updateById(assignment);

        Task task = taskMapper.selectById(assignment.getTaskId());
        Long pendingCount = taskAssignmentMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignment>()
                        .eq(TaskAssignment::getTaskId, task.getTaskId())
                        .eq(TaskAssignment::getStatus, "PENDING"));

        if (pendingCount == 0) {
            task.setStatus("CLOSED");
            taskMapper.updateById(task);

            notifyProducer.send(NotifyEvent.builder()
                    .type(NotifyType.TASK_COMPLETED)
                    .receiverId(task.getCreatorId())
                    .senderId(userId)
                    .title("任务已全部完成：" + task.getTitle())
                    .content("任务「" + task.getTitle() + "」的所有指派人已完成")
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .build());
        } else {
            User doneUser = userMapper.selectById(userId);
            notifyProducer.send(NotifyEvent.builder()
                    .type(NotifyType.TASK_COMPLETED)
                    .receiverId(task.getCreatorId())
                    .senderId(userId)
                    .title("部员完成任务：" + task.getTitle())
                    .content((doneUser != null ? doneUser.getName() : "部员") + "完成了任务「" + task.getTitle() + "」")
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .build());
        }

        log.info("任务标记完成: assignmentId={}, userId={}", assignmentId, userId);
    }

    @Transactional
    public void remind(Long assignmentId, Integer operatorId) {
        TaskAssignment assignment = taskAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ErrorCode.TASK_ASSIGNMENT_NOT_FOUND);
        }
        if ("DONE".equals(assignment.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_DONE);
        }

        Task task = taskMapper.selectById(assignment.getTaskId());
        int cooldown = task.getRemindCooldownMinutes();
        if (assignment.getLastRemindTime() != null) {
            Duration elapsed = Duration.between(assignment.getLastRemindTime(), LocalDateTime.now());
            long remainingMinutes = cooldown - elapsed.toMinutes();
            if (remainingMinutes > 0) {
                throw new BusinessException(ErrorCode.REMIND_COOLDOWN_ACTIVE, "冷却中，剩余 " + remainingMinutes + " 分钟");
            }
        }

        User operator = userMapper.selectById(operatorId);
        notifyProducer.send(NotifyEvent.builder()
                .type(NotifyType.TASK_REMIND)
                .receiverId(assignment.getAssigneeId())
                .senderId(operatorId)
                .title("任务催促：" + task.getTitle())
                .content((operator != null ? operator.getName() : "部长") + "催促你完成任务「" + task.getTitle() + "」")
                .refType("TASK")
                .refId(task.getTaskId())
                .build());

        assignment.setLastRemindTime(LocalDateTime.now());
        assignment.setRemindCount(assignment.getRemindCount() + 1);
        taskAssignmentMapper.updateById(assignment);

        TaskRemindLog logEntry = new TaskRemindLog();
        logEntry.setAssignmentId(assignmentId);
        logEntry.setOperatorId(operatorId);
        logEntry.setRemindTime(LocalDateTime.now());
        logEntry.setChannel("BOTH");
        taskRemindLogMapper.insert(logEntry);

        log.info("任务催促: assignmentId={}, operatorId={}", assignmentId, operatorId);
    }

    @Transactional
    public Task updateTask(Long taskId, Integer operatorId, String title, String description,
                           LocalDateTime dueTime, Integer priority, Integer remindCooldownMinutes) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getCreatorId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_CREATOR);
        }

        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (dueTime != null) task.setDueTime(dueTime);
        if (priority != null) task.setPriority(priority);
        if (remindCooldownMinutes != null) task.setRemindCooldownMinutes(remindCooldownMinutes);
        taskMapper.updateById(task);

        log.info("任务更新: taskId={}, operatorId={}", taskId, operatorId);
        return task;
    }

    @Transactional
    public void deleteTask(Long taskId, Integer operatorId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!task.getCreatorId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_CREATOR);
        }
        taskMapper.deleteById(taskId);
        log.info("任务删除: taskId={}, operatorId={}", taskId, operatorId);
    }

    public List<Map<String, Object>> getCandidates(Integer operatorId) {
        User operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        RoleHistoryParser.ParsedRole operatorRole = RoleHistoryParser.getLatestRole(operator);
        int currentYear = operatorRole.year();

        // SQL 层预过滤：只查询同届用户的 role_history（包含 "2024级"）
        // 格式统一为无空格：2024级部员&2025级部长，兼容旧格式带空格
        List<User> sameYearUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .isNotNull(User::getRoleHistory)
                        .ne(User::getRoleHistory, "")
                        .ne(User::getUserId, operatorId)
                        .like(User::getRoleHistory, currentYear + "级")
        );

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (User u : sameYearUsers) {
            try {
                List<String> roleEntries = RoleHistoryParser.parseRoleHistory(u.getRoleHistory());
                RoleHistoryParser.ParsedRole latestRole = null;
                RoleHistoryParser.ParsedRole currentYearRole = null;
                for (String entry : roleEntries) {
                    try {
                        RoleHistoryParser.ParsedRole parsed = RoleHistoryParser.parseSingleRole(entry);
                        if (latestRole == null) {
                            latestRole = parsed;
                        }
                        if (parsed.year() == currentYear) {
                            currentYearRole = parsed;
                            break;
                        }
                    } catch (BusinessException e) {
                        // skip malformed entries
                    }
                }
                if (currentYearRole != null) {
                    candidates.add(Map.of(
                            "userId", u.getUserId(),
                            "name", u.getName() != null ? u.getName() : "",
                            "username", u.getUsername(),
                            "studentId", u.getStudentId() != null ? u.getStudentId() : "",
                            "currentRole", currentYearRole.role()
                    ));
                }
            } catch (BusinessException e) {
                // skip users with parse errors
            }
        }
        return candidates;
    }
}
