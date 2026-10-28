package com.gymflow.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Tên ràng buộc trong Flyway V1 → thông báo dễ hiểu. Lớp cuối cùng chặn dữ liệu sai dù service bỏ sót. */
    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.ofEntries(
            Map.entry("uq_users_email", "Email đã được sử dụng"),
            Map.entry("uq_members_code", "Mã hội viên đã tồn tại"),
            Map.entry("uq_weight_member_day", "Đã có bản ghi cân nặng cho ngày này"),
            Map.entry("uq_plans_name", "Tên gói tập đã tồn tại"),
            Map.entry("uq_exercises_name", "Tên bài tập đã tồn tại"),
            Map.entry("uq_logs_schedule", "Buổi tập này đã được ghi nhận hoàn thành"),
            Map.entry("ck_ws_time", "Giờ kết thúc phải sau giờ bắt đầu"),
            Map.entry("ck_shift_time", "Giờ kết thúc ca phải sau giờ bắt đầu"),
            Map.entry("ck_sub_dates", "Ngày kết thúc gói phải sau ngày bắt đầu")
    );

    public record ErrorBody(LocalDateTime timestamp, int status, String message, Map<String, String> fieldErrors) {
    }

    private ResponseEntity<ErrorBody> body(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ErrorBody(LocalDateTime.now(), status.value(), message, fields));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApi(ApiException ex) {
        return body(ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(e -> fields.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorBody> handleConstraint(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return body(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", fields);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> handleIntegrity(DataIntegrityViolationException ex) {
        String raw = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        for (Map.Entry<String, String> e : CONSTRAINT_MESSAGES.entrySet()) {
            if (raw.contains(e.getKey())) return body(HttpStatus.CONFLICT, e.getValue(), null);
        }
        if (raw.contains("foreign key") || raw.contains("violates foreign")) {
            return body(HttpStatus.CONFLICT, "Dữ liệu đang được sử dụng ở nơi khác, không thể thực hiện", null);
        }
        if (raw.contains("check constraint")) {
            return body(HttpStatus.BAD_REQUEST, "Giá trị nằm ngoài phạm vi cho phép", null);
        }
        log.warn("Data integrity violation: {}", raw);
        return body(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm ràng buộc", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleUnreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "Định dạng dữ liệu gửi lên không đúng", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorBody> handleMismatch(MethodArgumentTypeMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, "Tham số '" + ex.getName() + "' không hợp lệ", null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorBody> handleBadCredentials(BadCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleDenied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập chức năng này", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleOther(Exception ex) {
        log.error("Unhandled error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi hệ thống, vui lòng thử lại sau", null);
    }
}
