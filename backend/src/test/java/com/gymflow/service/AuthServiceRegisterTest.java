package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.config.DemoAccounts;
import com.gymflow.domain.Member;
import com.gymflow.domain.Role;
import com.gymflow.domain.User;
import com.gymflow.domain.UserStatus;
import com.gymflow.dto.Dtos.AuthOptions;
import com.gymflow.dto.Dtos.RegisterRequest;
import com.gymflow.dto.Dtos.RegisterResponse;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.PasswordResetTokenRepository;
import com.gymflow.repository.TrainerRepository;
import com.gymflow.repository.UserRepository;
import com.gymflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock UserRepository userRepository;
    @Mock MemberRepository memberRepository;
    @Mock TrainerRepository trainerRepository;
    @Mock PasswordResetTokenRepository resetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuditService auditService;
    @Mock UserService userService;
    @Mock MemberService memberService;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, memberRepository, trainerRepository, resetTokenRepository,
                passwordEncoder, jwtService, auditService, userService, memberService, Clock.systemDefaultZone());
        ReflectionTestUtils.setField(service, "registrationEnabled", true);
        ReflectionTestUtils.setField(service, "demoQuickLogin", true);
    }

    private static RegisterRequest req(String pw, String confirm, boolean terms) {
        return new RegisterRequest("Nguyễn Văn A", "New.Member@Example.com", "0912345678", pw, confirm, terms);
    }

    @Test
    void registerAlwaysCreatesMemberRole() {
        User u = new User();
        u.setId(50L);
        u.setEmail("new.member@example.com");
        u.setRole(Role.MEMBER);
        when(userService.createAccount(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new UserService.Created(u, null));
        when(memberService.createSelfRegistered(u)).thenReturn(new Member());

        RegisterResponse res = service.register(req("Strong123", "Strong123", true));

        verify(userService).createAccount("New.Member@Example.com", "Nguyễn Văn A", "0912345678", "Strong123", Role.MEMBER);
        verify(memberService).createSelfRegistered(u);
        assertThat(res.email()).isEqualTo("new.member@example.com");
    }

    @Test
    void registerRejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> service.register(req("Strong123", "Strong124", true)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("không khớp");
        verifyNoInteractions(userService, memberService);
    }

    @Test
    void registerRequiresTerms() {
        assertThatThrownBy(() -> service.register(req("Strong123", "Strong123", false)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Điều khoản");
        verifyNoInteractions(userService, memberService);
    }

    @Test
    void registerBlockedWhenDisabled() {
        ReflectionTestUtils.setField(service, "registrationEnabled", false);
        assertThatThrownBy(() -> service.register(req("Strong123", "Strong123", true)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(userService, memberService);
    }

    @Test
    void duplicateEmailConflictPropagatesAndNoMemberIsCreated() {
        when(userService.createAccount(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(ApiException.conflict("Email đã được sử dụng"));
        assertThatThrownBy(() -> service.register(req("Strong123", "Strong123", true)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(memberService);
    }

    @Test
    void optionsListOnlyDemoAccountsThatExistAndAreActive() {
        User admin = user(DemoAccounts.ADMIN.email(), Role.ADMIN, UserStatus.ACTIVE);
        User lockedTrainer = user(DemoAccounts.TRAINER.email(), Role.TRAINER, UserStatus.LOCKED);
        when(userRepository.findByEmailIgnoreCase(DemoAccounts.ADMIN.email())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailIgnoreCase(DemoAccounts.TRAINER.email())).thenReturn(Optional.of(lockedTrainer));
        when(userRepository.findByEmailIgnoreCase(DemoAccounts.MEMBER.email())).thenReturn(Optional.empty());

        AuthOptions o = service.options();

        assertThat(o.registrationEnabled()).isTrue();
        assertThat(o.demoAccounts()).extracting(a -> a.email()).containsExactly(DemoAccounts.ADMIN.email());
    }

    @Test
    void optionsHideDemoAccountsWhenQuickLoginOff() {
        ReflectionTestUtils.setField(service, "demoQuickLogin", false);
        assertThat(service.options().demoAccounts()).isEmpty();
        verifyNoInteractions(userRepository);
    }

    private static User user(String email, Role role, UserStatus status) {
        User u = new User();
        u.setEmail(email);
        u.setRole(role);
        u.setStatus(status);
        return u;
    }
}
