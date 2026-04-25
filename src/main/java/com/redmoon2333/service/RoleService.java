package com.redmoon2333.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.redmoon2333.entity.RoleChangeLog;
import com.redmoon2333.entity.User;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.RoleChangeLogMapper;
import com.redmoon2333.mapper.UserMapper;
import com.redmoon2333.util.RoleHistoryParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserMapper userMapper;
    private final RoleChangeLogMapper roleChangeLogMapper;

    public IPage<User> getUsers(Integer page, Integer pageSize) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(User::getRoleHistory).ne(User::getRoleHistory, "");
        wrapper.orderByDesc(User::getUserId);
        return userMapper.selectPage(pageParam, wrapper);
    }

    @Transactional
    public void appointMinister(Integer targetUserId, Integer operatorId, Integer year, String reason) {
        User operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        RoleHistoryParser.ParsedRole operatorRole = RoleHistoryParser.getLatestRole(operator);
        if (operatorRole.year() != year) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS, "无法跨届任命");
        }
        if (!operatorRole.role().equals("部长")) {
            throw new BusinessException(ErrorCode.ROLE_DEPUTY_CANNOT_APPOINT_MINISTER);
        }

        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(User::getRoleHistory).ne(User::getRoleHistory, "");
        List<User> allUsers = userMapper.selectList(wrapper);

        for (User u : allUsers) {
            try {
                RoleHistoryParser.ParsedRole role = RoleHistoryParser.getLatestRole(u);
                if (role.year() == year && role.role().equals("部长") && !u.getUserId().equals(targetUserId)) {
                    throw new BusinessException(ErrorCode.ROLE_MINISTER_UNIQUE_VIOLATION);
                }
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.ROLE_MINISTER_UNIQUE_VIOLATION) {
                    throw e;
                }
            }
        }

        String beforeHistory = targetUser.getRoleHistory();
        targetUser.setRoleHistory(RoleHistoryParser.appendRole(targetUser.getRoleHistory(), year, "部长"));
        userMapper.updateById(targetUser);

        RoleChangeLog logEntry = new RoleChangeLog();
        logEntry.setTargetUserId(targetUserId);
        logEntry.setOperatorId(operatorId);
        logEntry.setBeforeRoleHistory(beforeHistory);
        logEntry.setAfterRoleHistory(targetUser.getRoleHistory());
        logEntry.setReason(reason);
        logEntry.setChangeTime(LocalDateTime.now());
        roleChangeLogMapper.insert(logEntry);

        log.info("任命部长: targetUserId={}, year={}, operatorId={}", targetUserId, year, operatorId);
    }

    @Transactional
    public void appointDeputy(Integer targetUserId, Integer operatorId, Integer year, String reason) {
        User operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        RoleHistoryParser.ParsedRole operatorRole = RoleHistoryParser.getLatestRole(operator);
        if (operatorRole.year() != year) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS, "无法跨届任命");
        }
        if (operatorRole.role().equals("部员")) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS, "部员无权任命副部长");
        }

        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String beforeHistory = targetUser.getRoleHistory();
        targetUser.setRoleHistory(RoleHistoryParser.appendRole(targetUser.getRoleHistory(), year, "副部长"));
        userMapper.updateById(targetUser);

        RoleChangeLog logEntry = new RoleChangeLog();
        logEntry.setTargetUserId(targetUserId);
        logEntry.setOperatorId(operatorId);
        logEntry.setBeforeRoleHistory(beforeHistory);
        logEntry.setAfterRoleHistory(targetUser.getRoleHistory());
        logEntry.setReason(reason);
        logEntry.setChangeTime(LocalDateTime.now());
        roleChangeLogMapper.insert(logEntry);

        log.info("任命副部长: targetUserId={}, year={}, operatorId={}", targetUserId, year, operatorId);
    }

    @Transactional
    public void updateRole(Integer targetUserId, Integer operatorId, Integer year, String newRole, String reason) {
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!newRole.equals("部长") && !newRole.equals("副部长") && !newRole.equals("部员")) {
            throw new BusinessException(ErrorCode.ROLE_INVALID);
        }

        User operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        RoleHistoryParser.ParsedRole operatorRole = RoleHistoryParser.getLatestRole(operator);
        if (operatorRole.year() != year) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS, "无法跨届任命");
        }

        if (newRole.equals("部长")) {
            if (!operatorRole.role().equals("部长")) {
                throw new BusinessException(ErrorCode.ROLE_DEPUTY_CANNOT_APPOINT_MINISTER);
            }
            appointMinister(targetUserId, operatorId, year, reason);
            return;
        }

        if (newRole.equals("副部长")) {
            if (operatorRole.role().equals("部员")) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS, "部员无权任命副部长");
            }
        }

        String beforeHistory = targetUser.getRoleHistory();
        targetUser.setRoleHistory(RoleHistoryParser.appendRole(targetUser.getRoleHistory(), year, newRole));
        userMapper.updateById(targetUser);

        RoleChangeLog logEntry = new RoleChangeLog();
        logEntry.setTargetUserId(targetUserId);
        logEntry.setOperatorId(operatorId);
        logEntry.setBeforeRoleHistory(beforeHistory);
        logEntry.setAfterRoleHistory(targetUser.getRoleHistory());
        logEntry.setReason(reason);
        logEntry.setChangeTime(LocalDateTime.now());
        roleChangeLogMapper.insert(logEntry);

        log.info("更新角色: targetUserId={}, year={}, newRole={}, operatorId={}", targetUserId, year, newRole, operatorId);
    }
}
