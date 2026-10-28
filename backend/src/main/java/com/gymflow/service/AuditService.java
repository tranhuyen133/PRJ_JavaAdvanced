package com.gymflow.service;

import com.gymflow.domain.AuditLog;
import com.gymflow.repository.AuditLogRepository;
import com.gymflow.security.AuthUser;
import com.gymflow.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Ghi nhật ký thao tác. REQUIRES_NEW: nhật ký vẫn được lưu kể cả khi giao dịch chính rollback
 * (ví dụ: đăng nhập sai vẫn phải để lại dấu vết).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String targetType, Object targetId, String detail) {
        AuthUser actor = CurrentUser.orNull();
        write(actor == null ? null : actor.id(), actor == null ? null : actor.email(), action, targetType, targetId, detail, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAs(Long actorId, String actorEmail, String action, String targetType, Object targetId,
                      String detail, boolean success) {
        write(actorId, actorEmail, action, targetType, targetId, detail, success);
    }

    private void write(Long actorId, String actorEmail, String action, String targetType, Object targetId,
                       String detail, boolean success) {
        try {
            AuditLog a = new AuditLog();
            a.setActorId(actorId);
            a.setActorEmail(actorEmail);
            a.setAction(action);
            a.setTargetType(targetType);
            a.setTargetId(targetId == null ? null : String.valueOf(targetId));
            a.setDetail(detail == null ? null : detail.length() > 500 ? detail.substring(0, 500) : detail);
            a.setSuccess(success);
            a.setIp(clientIp());
            repository.save(a);
        } catch (RuntimeException e) {
            // Nhật ký không được làm hỏng nghiệp vụ chính.
            log.warn("Không ghi được audit log {}: {}", action, e.getMessage());
        }
    }

    private String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return null;
        HttpServletRequest req = attrs.getRequest();
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
