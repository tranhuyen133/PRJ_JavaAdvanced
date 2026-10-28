package com.gymflow.web;

import com.gymflow.common.PageResponse;
import com.gymflow.domain.Role;
import com.gymflow.domain.UserStatus;
import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.DashboardService;
import com.gymflow.service.NotificationService;
import com.gymflow.service.ReportService;
import com.gymflow.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Tổng quan, báo cáo & quản trị")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final UserService userService;

    // ---------- Dashboard ----------
    @GetMapping("/api/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard adminDashboard() {
        return dashboardService.admin();
    }

    @GetMapping("/api/dashboard/trainer")
    @PreAuthorize("hasRole('TRAINER')")
    public TrainerDashboard trainerDashboard() {
        return dashboardService.trainer(CurrentUser.get());
    }

    @GetMapping("/api/dashboard/member")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberDashboard memberDashboard() {
        return dashboardService.member(CurrentUser.get());
    }

    @GetMapping("/api/notifications")
    public List<NotificationResponse> notifications() {
        return notificationService.forUser(CurrentUser.get());
    }

    // ---------- Báo cáo ----------
    @GetMapping("/api/reports/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse report(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(from, to);
    }

    // ---------- Tài khoản ----------
    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> users(@RequestParam(defaultValue = "") String q,
                                            @RequestParam(required = false) Role role,
                                            @RequestParam(required = false) UserStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return userService.search(q, role, status, page, size);
    }

    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserCreatedResponse createAdmin(@Valid @RequestBody AdminCreateRequest req) {
        return userService.createAdmin(req);
    }

    @PutMapping("/api/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse userStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest req) {
        return userService.setStatus(CurrentUser.get(), id, req.status());
    }

    @PostMapping("/api/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public TempPasswordResponse resetPassword(@PathVariable Long id) {
        return userService.resetPassword(CurrentUser.get(), id);
    }

    // ---------- Nhật ký ----------
    @GetMapping("/api/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditResponse> audit(@RequestParam(defaultValue = "") String q,
                                             @RequestParam(defaultValue = "") String action,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "30") int size) {
        return notificationService.audit(q, action, from, to, page, size);
    }

    @GetMapping("/api/audit-logs/actions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<String> auditActions() {
        return notificationService.auditActions();
    }
}
