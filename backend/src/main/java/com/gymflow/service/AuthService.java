package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.common.Secrets;
import com.gymflow.config.DemoAccounts;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.PasswordResetTokenRepository;
import com.gymflow.repository.TrainerRepository;
import com.gymflow.repository.UserRepository;
import com.gymflow.security.AuthUser;
import com.gymflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BAD_LOGIN = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final TrainerRepository trainerRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final UserService userService;
    private final MemberService memberService;
    private final Clock clock;

    @Value("${app.reset.expose-token:false}")
    private boolean exposeResetToken;

    @Value("${app.reset.valid-minutes:30}")
    private int resetValidMinutes;

    @Value("${app.registration.enabled:true}")
    private boolean registrationEnabled;

    @Value("${app.demo.quick-login:false}")
    private boolean demoQuickLogin;

    /** Cấu hình công khai cho trang đăng nhập / đăng ký. */
    @Transactional(readOnly = true)
    public AuthOptions options() {
        List<DemoAccount> demo = !demoQuickLogin ? List.of() : DemoAccounts.ALL.stream()
                .filter(a -> userRepository.findByEmailIgnoreCase(a.email())
                        .filter(u -> u.getStatus() == UserStatus.ACTIVE && u.getRole() == a.role())
                        .isPresent())
                .toList();
        return new AuthOptions(registrationEnabled, demo);
    }

    /**
     * Hội viên tự đăng ký. Vai trò luôn là MEMBER, trạng thái ACTIVE; chưa có gói tập (mua tại quầy).
     * Không tự đăng nhập: người dùng quay lại trang đăng nhập với email vừa tạo.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (!registrationEnabled) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Đăng ký trực tuyến đang tạm đóng, vui lòng liên hệ quầy lễ tân");
        }
        if (!req.acceptTerms()) {
            throw ApiException.badRequest("Bạn cần đồng ý Điều khoản sử dụng và Chính sách bảo mật");
        }
        if (!req.password().equals(req.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu nhập lại không khớp");
        }
        UserService.Created c = userService.createAccount(req.email(), req.fullName(), req.phone(), req.password(), Role.MEMBER);
        memberService.createSelfRegistered(c.user());
        return new RegisterResponse("Đăng ký thành công! Bạn có thể đăng nhập ngay.", c.user().getEmail());
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        Optional<User> found = userRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty() || !passwordEncoder.matches(req.password(), found.get().getPasswordHash())) {
            auditService.logAs(found.map(User::getId).orElse(null), email, "LOGIN_FAILED", "USER",
                    found.map(User::getId).orElse(null), "Sai email hoặc mật khẩu", false);
            throw ApiException.unauthorized(BAD_LOGIN);
        }
        User user = found.get();
        if (user.getStatus() == UserStatus.LOCKED) {
            auditService.logAs(user.getId(), email, "LOGIN_FAILED", "USER", user.getId(), "Tài khoản đang bị khoá", false);
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khoá, vui lòng liên hệ quản lý");
        }
        user.setLastLoginAt(LocalDateTime.now(clock));
        auditService.logAs(user.getId(), user.getEmail(), "LOGIN", "USER", user.getId(), null, true);
        return new LoginResponse(jwtService.generate(user), jwtService.getExpirationMinutes(), me(user));
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthUser principal) {
        return me(userRepository.findById(principal.id()).orElseThrow(() -> ApiException.notFound("tài khoản")));
    }

    private MeResponse me(User u) {
        Long memberId = u.getRole() == Role.MEMBER ? memberRepository.findByUserId(u.getId()).map(Member::getId).orElse(null) : null;
        Long trainerId = u.getRole() == Role.TRAINER ? trainerRepository.findByUserId(u.getId()).map(Trainer::getId).orElse(null) : null;
        return new MeResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), memberId, trainerId,
                u.getLastLoginAt());
    }

    @Transactional
    public MeResponse updateProfile(AuthUser principal, UpdateProfileRequest req) {
        User u = userRepository.findById(principal.id()).orElseThrow(() -> ApiException.notFound("tài khoản"));
        u.setFullName(req.fullName().trim());
        u.setPhone(Mappers.blankToNull(req.phone()));
        auditService.log("PROFILE_UPDATE", "USER", u.getId(), null);
        return me(u);
    }

    @Transactional
    public void changePassword(AuthUser principal, ChangePasswordRequest req) {
        User u = userRepository.findById(principal.id()).orElseThrow(() -> ApiException.notFound("tài khoản"));
        if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(req.newPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        auditService.log("PASSWORD_CHANGE", "USER", u.getId(), null);
    }

    /** Luôn trả cùng một thông báo để không lộ email nào đã đăng ký. */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req) {
        String generic = "Nếu email tồn tại trong hệ thống, mã khôi phục đã được gửi.";
        Optional<User> found = userRepository.findByEmailIgnoreCase(req.email().trim());
        if (found.isEmpty() || found.get().getStatus() == UserStatus.LOCKED) {
            return new ForgotPasswordResponse(generic, null, resetValidMinutes);
        }
        User u = found.get();
        String token = Secrets.resetToken();
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(u);
        t.setTokenHash(Secrets.sha256(token));
        t.setExpiresAt(LocalDateTime.now(clock).plusMinutes(resetValidMinutes));
        resetTokenRepository.save(t);
        auditService.logAs(u.getId(), u.getEmail(), "PASSWORD_RESET_REQUEST", "USER", u.getId(), null, true);
        if (exposeResetToken) {
            log.info("[DEMO] Mã khôi phục cho {}: {}", u.getEmail(), token);
            return new ForgotPasswordResponse(generic, token, resetValidMinutes);
        }
        return new ForgotPasswordResponse(generic, null, resetValidMinutes);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken t = resetTokenRepository.findByTokenHash(Secrets.sha256(req.token()))
                .orElseThrow(() -> ApiException.badRequest("Mã khôi phục không đúng"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (t.getUsedAt() != null) throw ApiException.badRequest("Mã khôi phục đã được sử dụng");
        if (t.getExpiresAt().isBefore(now)) throw ApiException.badRequest("Mã khôi phục đã hết hạn");
        User u = t.getUser();
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        t.setUsedAt(now);
        auditService.logAs(u.getId(), u.getEmail(), "PASSWORD_RESET", "USER", u.getId(), null, true);
    }
}
