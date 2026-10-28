package com.gymflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Giới hạn số lần gọi các API công khai nhạy cảm theo IP (cửa sổ cố định, lưu trong bộ nhớ):
 * chống dò mật khẩu, tạo tài khoản hàng loạt và spam email khôi phục.
 * IP lấy từ X-Real-IP do nginx ghi đè; ở production cổng backend không mở ra ngoài nên header không giả được.
 * Chạy một instance là đủ; nếu scale nhiều instance cần chuyển sang bộ đếm dùng chung (Redis…).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    record Rule(String bucket, int limit, long windowMillis) {}

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule("login", 10, 60_000),
            "/api/auth/register", new Rule("register", 5, 600_000),
            "/api/auth/forgot-password", new Rule("reset", 5, 600_000),
            "/api/auth/reset-password", new Rule("reset", 5, 600_000));

    private static final int MAX_KEYS = 10_000;

    private static final class Window {
        final long start;
        int count;
        Window(long start) { this.start = start; }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final LongSupplier now;

    @Autowired
    public AuthRateLimitFilter(ObjectMapper objectMapper, @Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this(objectMapper, enabled, System::currentTimeMillis);
    }

    AuthRateLimitFilter(ObjectMapper objectMapper, boolean enabled, LongSupplier now) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.now = now;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !RULES.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = RULES.get(request.getRequestURI());
        long t = now.getAsLong();
        if (windows.size() > MAX_KEYS) {
            windows.entrySet().removeIf(e -> t - e.getValue().start >= RULES.values().stream()
                    .mapToLong(Rule::windowMillis).max().orElse(0));
        }
        String key = rule.bucket() + "|" + clientIp(request);
        Window w = windows.compute(key, (k, cur) -> {
            Window next = cur == null || t - cur.start >= rule.windowMillis() ? new Window(t) : cur;
            next.count++;
            return next;
        });
        if (w.count > rule.limit()) {
            long retry = Math.max(1, (w.start + rule.windowMillis() - t + 999) / 1000);
            reject(response, retry);
            return;
        }
        chain.doFilter(request, response);
    }

    static String clientIp(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real.trim() : request.getRemoteAddr();
    }

    private void reject(HttpServletResponse res, long retrySeconds) throws IOException {
        res.setStatus(429);
        res.setHeader("Retry-After", String.valueOf(retrySeconds));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 429);
        body.put("message", "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau " + retrySeconds + " giây.");
        objectMapper.writeValue(res.getWriter(), body);
    }
}
