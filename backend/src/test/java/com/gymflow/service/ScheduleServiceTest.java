package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.ScheduleCreatedResponse;
import com.gymflow.dto.Dtos.ScheduleRequest;
import com.gymflow.repository.TrainerRepository;
import com.gymflow.repository.WorkoutLogRepository;
import com.gymflow.repository.WorkoutPlanRepository;
import com.gymflow.repository.WorkoutScheduleRepository;
import com.gymflow.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 9, 0);

    @Mock WorkoutScheduleRepository scheduleRepository;
    @Mock WorkoutLogRepository logRepository;
    @Mock WorkoutPlanRepository planRepository;
    @Mock TrainerRepository trainerRepository;
    @Mock SubscriptionService subscriptionService;
    @Mock AccessService access;
    @Mock AuditService auditService;
    @Mock Mappers mappers;

    ScheduleService service;
    Member member;
    Trainer trainer;
    AuthUser trainerUser = new AuthUser(5L, "trainer@gymflow.vn", Role.TRAINER, "HLV");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new ScheduleService(scheduleRepository, logRepository, planRepository, trainerRepository,
                subscriptionService, access, auditService, mappers, clock);
        User tu = new User();
        tu.setId(5L);
        tu.setFullName("Lê Hoàng Phúc");
        trainer = new Trainer();
        trainer.setId(3L);
        trainer.setUser(tu);
        trainer.setActive(true);
        member = new Member();
        member.setId(1L);
        member.setCode("HV00001");
        member.setStatus(MemberStatus.ACTIVE);
        member.setTrainer(trainer);
        lenient().when(access.requireCoach(trainerUser, 1L)).thenReturn(member);
        lenient().when(access.requireTrainerOf(trainerUser)).thenReturn(trainer);
        lenient().when(scheduleRepository.saveAndFlush(any())).thenAnswer(inv -> {
            WorkoutSchedule s = inv.getArgument(0);
            s.setId((long) (Math.random() * 1_000_000));
            return s;
        });
        lenient().when(mappers.schedules(anyList())).thenReturn(List.of());
    }

    private ScheduleRequest request(LocalDateTime start, int minutes, int repeat) {
        return new ScheduleRequest(1L, null, null, "Buổi 1", start, start.plusMinutes(minutes), null, repeat);
    }

    @Test
    void rejectsSessionWithoutActivePackage() {
        when(subscriptionService.hasEffective(eq(1L), any())).thenReturn(false);
        assertThatThrownBy(() -> service.create(trainerUser, request(NOW.plusDays(1), 60, 0)))
                .isInstanceOf(ApiException.class).hasMessageContaining("gói tập hiệu lực");
    }

    @Test
    void rejectsTrainerDoubleBooking() {
        when(subscriptionService.hasEffective(eq(1L), any())).thenReturn(true);
        when(scheduleRepository.memberBusy(any(), any(), any(), any(), any())).thenReturn(false);
        when(scheduleRepository.trainerBusy(eq(3L), any(), any(), isNull(), eq(ScheduleStatus.CANCELLED))).thenReturn(true);
        assertThatThrownBy(() -> service.create(trainerUser, request(NOW.plusDays(1), 60, 0)))
                .isInstanceOf(ApiException.class).hasMessageContaining("trùng giờ");
    }

    @Test
    void rejectsPastTimeAndBadDuration() {
        assertThatThrownBy(() -> service.create(trainerUser, request(NOW.minusHours(2), 60, 0)))
                .hasMessageContaining("đã qua");
        assertThatThrownBy(() -> service.create(trainerUser, request(NOW.plusDays(1), 10, 0)))
                .hasMessageContaining("từ 15 đến 240 phút");
    }

    @Test
    void weeklyRepeatSkipsConflictingWeeksInsteadOfFailingAll() {
        LocalDateTime start = NOW.plusDays(1).withHour(18);
        when(subscriptionService.hasEffective(eq(1L), any())).thenReturn(true);
        when(scheduleRepository.trainerBusy(any(), any(), any(), any(), any())).thenReturn(false);
        when(scheduleRepository.memberBusy(any(), any(), any(), any(), any())).thenReturn(false);
        // Tuần thứ 3 hội viên đã có lịch khác.
        when(scheduleRepository.memberBusy(eq(1L), eq(start.plusWeeks(2)), any(), any(), any())).thenReturn(true);

        ScheduleCreatedResponse res = service.create(trainerUser, request(start, 60, 3));

        verify(scheduleRepository, times(3)).saveAndFlush(any());
        assertThat(res.skipped()).hasSize(1);
        assertThat(res.skipped().get(0)).contains("trùng giờ");
    }

    @Test
    void cannotCompleteFutureSession() {
        WorkoutSchedule s = new WorkoutSchedule();
        s.setId(10L);
        s.setMember(member);
        s.setStatus(ScheduleStatus.SCHEDULED);
        s.setStartTime(NOW.plusHours(3));
        s.setEndTime(NOW.plusHours(4));
        when(scheduleRepository.findById(10L)).thenReturn(java.util.Optional.of(s));
        when(access.canView(trainerUser, member)).thenReturn(true);
        assertThatThrownBy(() -> service.complete(trainerUser, 10L, new com.gymflow.dto.Dtos.CompleteRequest(null, null, null, null)))
                .hasMessageContaining("Chưa đến giờ tập");
        verify(logRepository, never()).save(any());
    }
}
