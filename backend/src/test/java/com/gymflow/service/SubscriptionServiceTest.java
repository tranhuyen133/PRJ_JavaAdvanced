package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.SubscriptionPreview;
import com.gymflow.dto.Dtos.SubscriptionRequest;
import com.gymflow.repository.MemberSubscriptionRepository;
import com.gymflow.repository.UserRepository;
import com.gymflow.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    @Mock MemberSubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;
    @Mock PlanService planService;
    @Mock AccessService access;
    @Mock AuditService auditService;
    @Mock Mappers mappers;

    SubscriptionService service;
    Member member;
    MembershipPlan plan;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE);
        service = new SubscriptionService(subscriptionRepository, userRepository, planService, access, auditService, mappers, clock);
        member = new Member();
        member.setId(1L);
        member.setCode("HV00001");
        member.setStatus(MemberStatus.ACTIVE);
        plan = new MembershipPlan();
        plan.setId(2L);
        plan.setName("Gói 1 tháng");
        plan.setDurationDays(30);
        plan.setPrice(500_000L);
        plan.setActive(true);
        lenient().when(mappers.today()).thenReturn(TODAY);
        lenient().when(access.loadMember(1L)).thenReturn(member);
        lenient().when(planService.load(2L)).thenReturn(plan);
    }

    @Test
    void previewStartsTodayWhenNoCurrentPackage() {
        when(subscriptionRepository.maxEndDate(1L)).thenReturn(TODAY.minusDays(3));
        SubscriptionPreview p = service.preview(1L, 2L, null);
        assertThat(p.startDate()).isEqualTo(TODAY);
        assertThat(p.endDate()).isEqualTo(TODAY.plusDays(29));
        assertThat(p.chained()).isFalse();
    }

    @Test
    void renewalChainsAfterCurrentPackage() {
        when(subscriptionRepository.maxEndDate(1L)).thenReturn(TODAY.plusDays(5));
        SubscriptionPreview p = service.preview(1L, 2L, null);
        assertThat(p.startDate()).isEqualTo(TODAY.plusDays(6));
        assertThat(p.endDate()).isEqualTo(TODAY.plusDays(35));
        assertThat(p.chained()).isTrue();
    }

    @Test
    void overlappingPackageIsRejected() {
        when(subscriptionRepository.existsOverlap(1L, TODAY, TODAY.plusDays(29))).thenReturn(true);
        AuthUser admin = new AuthUser(9L, "admin@gymflow.vn", Role.ADMIN, "Admin");
        assertThatThrownBy(() -> service.create(admin, new SubscriptionRequest(1L, 2L, TODAY, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("trùng");
        verify(subscriptionRepository, never()).saveAndFlush(any());
    }

    @Test
    void inactivePlanCannotBeSold() {
        plan.setActive(false);
        AuthUser admin = new AuthUser(9L, "admin@gymflow.vn", Role.ADMIN, "Admin");
        assertThatThrownBy(() -> service.create(admin, new SubscriptionRequest(1L, 2L, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ngừng cung cấp");
    }

    @Test
    void createUsesPlanPriceByDefault() {
        when(subscriptionRepository.maxEndDate(1L)).thenReturn(null);
        when(subscriptionRepository.existsOverlap(any(), any(), any())).thenReturn(false);
        when(subscriptionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        AuthUser admin = new AuthUser(9L, "admin@gymflow.vn", Role.ADMIN, "Admin");
        service.create(admin, new SubscriptionRequest(1L, 2L, null, null, null));
        verify(subscriptionRepository).saveAndFlush(argThat(s -> s.getPricePaid() == 500_000L
                && s.getStartDate().equals(TODAY) && s.getEndDate().equals(TODAY.plusDays(29))));
    }

    @Test
    void memberRowIsLockedBeforeOverlapCheck() {
        when(subscriptionRepository.maxEndDate(1L)).thenReturn(null);
        when(subscriptionRepository.existsOverlap(any(), any(), any())).thenReturn(false);
        when(subscriptionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        AuthUser admin = new AuthUser(9L, "admin@gymflow.vn", Role.ADMIN, "Admin");
        service.create(admin, new SubscriptionRequest(1L, 2L, null, null, null));
        InOrder order = inOrder(subscriptionRepository);
        order.verify(subscriptionRepository).lockMember(1L);
        order.verify(subscriptionRepository).maxEndDate(1L);
        order.verify(subscriptionRepository).existsOverlap(1L, TODAY, TODAY.plusDays(29));
        order.verify(subscriptionRepository).saveAndFlush(any());
    }
}
