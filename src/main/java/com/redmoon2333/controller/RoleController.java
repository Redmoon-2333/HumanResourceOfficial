package com.redmoon2333.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.entity.User;
import com.redmoon2333.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/users")
    @RequireMinisterRole
    public ApiResponse<IPage<User>> getUsers(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        IPage<User> result = roleService.getUsers(page, pageSize);
        return ApiResponse.success(result);
    }

    @PutMapping("/users/{userId}")
    @RequireMinisterRole
    public ApiResponse<Void> updateRole(
            HttpServletRequest request,
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        Integer year = (Integer) body.get("year");
        String newRole = (String) body.get("newRole");
        String reason = (String) body.get("reason");
        roleService.updateRole(userId, operatorId, year, newRole, reason);
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/appoint-minister")
    @RequireMinisterRole
    public ApiResponse<Void> appointMinister(
            HttpServletRequest request,
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        Integer year = (Integer) body.get("year");
        String reason = (String) body.get("reason");
        roleService.appointMinister(userId, operatorId, year, reason);
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/appoint-deputy")
    @RequireMinisterRole
    public ApiResponse<Void> appointDeputy(
            HttpServletRequest request,
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> body) {
        Integer operatorId = (Integer) request.getAttribute("userId");
        Integer year = (Integer) body.get("year");
        String reason = (String) body.get("reason");
        roleService.appointDeputy(userId, operatorId, year, reason);
        return ApiResponse.success(null);
    }
}
