package com.gymflow.web;

import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/options")
    public AuthOptions options() {
        return authService.options();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me(CurrentUser.get());
    }

    @PutMapping("/me")
    public MeResponse updateMe(@Valid @RequestBody UpdateProfileRequest req) {
        return authService.updateProfile(CurrentUser.get(), req);
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(CurrentUser.get(), req);
        return new MessageResponse("Đã đổi mật khẩu");
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        return authService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return new MessageResponse("Đặt lại mật khẩu thành công, vui lòng đăng nhập");
    }
}
