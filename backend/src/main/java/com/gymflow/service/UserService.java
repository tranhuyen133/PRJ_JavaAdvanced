package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.common.PageResponse;
import com.gymflow.common.Secrets;
import com.gymflow.domain.Role;
import com.gymflow.domain.User;
import com.gymflow.domain.UserStatus;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.UserRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Mappers mappers;

    /** Kết quả tạo tài khoản: mật khẩu tạm chỉ khác null khi hệ thống tự sinh. */
    public record Created(User user, String temporaryPassword) {
    }

    /** Dùng chung khi tạo hội viên, HLV, quản lý. */
    @Transactional
    public Created createAccount(String email, String fullName, String phone, String password, Role role) {
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        String raw = Mappers.blankToNull(password);
        String temp = raw == null ? Secrets.temporaryPassword() : null;
        User u = new User();
        u.setEmail(normalized);
        u.setFullName(fullName.trim());
        u.setPhone(Mappers.blankToNull(phone));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setPasswordHash(passwordEncoder.encode(raw == null ? temp : raw));
        return new Created(userRepository.save(u), temp);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, Role role, UserStatus status, int page, int size) {
        return PageResponse.of(userRepository.search(q == null ? "" : q.trim(), role, status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), mappers::user);
    }

    @Transactional
    public UserCreatedResponse createAdmin(AdminCreateRequest req) {
        Created c = createAccount(req.email(), req.fullName(), req.phone(), req.password(), Role.ADMIN);
        auditService.log("USER_CREATE", "USER", c.user().getId(), "Tạo tài khoản quản lý " + c.user().getEmail());
        return new UserCreatedResponse(mappers.user(c.user()), c.temporaryPassword());
    }

    @Transactional
    public UserResponse setStatus(AuthUser actor, Long id, UserStatus status) {
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("tài khoản"));
        if (u.getId().equals(actor.id()) && status == UserStatus.LOCKED) {
            throw ApiException.badRequest("Không thể tự khoá tài khoản của chính mình");
        }
        if (u.getRole() == Role.ADMIN && status == UserStatus.LOCKED
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.badRequest("Hệ thống cần ít nhất một tài khoản quản lý");
        }
        u.setStatus(status);
        auditService.log(status == UserStatus.LOCKED ? "USER_LOCK" : "USER_UNLOCK", "USER", u.getId(), u.getEmail());
        return mappers.user(u);
    }

    @Transactional
    public TempPasswordResponse resetPassword(AuthUser actor, Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("tài khoản"));
        String temp = Secrets.temporaryPassword();
        u.setPasswordHash(passwordEncoder.encode(temp));
        auditService.log("USER_RESET_PASSWORD", "USER", u.getId(), u.getEmail());
        return new TempPasswordResponse(temp);
    }
}
