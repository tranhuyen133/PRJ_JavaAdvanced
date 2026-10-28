package com.gymflow.service;

import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.*;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository memberRepository;
    private final TrainerRepository trainerRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final WorkoutPlanRepository planRepository;
    private final WeightRecordRepository weightRepository;
    private final AuditLogRepository auditRepository;
    private final java.time.Clock clock;
    private final AccessService access;
    private final Mappers mappers;

    public AdminDashboard admin() {
        LocalDate today = mappers.today();
        YearMonth month = YearMonth.from(today);
        List<LabelValue> revenue = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = month.minusMonths(i);
            revenue.add(new LabelValue(String.format("%02d/%d", ym.getMonthValue(), ym.getYear()), revenueOf(ym)));
        }
        List<MemberSubscription> effective = subscriptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(s -> s.isEffectiveOn(today)).toList();
        Map<String, Long> byPlan = new TreeMap<>();
        effective.forEach(s -> byPlan.merge(s.getPlan().getName(), 1L, Long::sum));
        List<MemberSubscription> expiring = subscriptionRepository.findEndingBetween(today, today.plusDays(MemberSubscription.EXPIRING_DAYS));
        LocalDateTime dayStart = today.atStartOfDay();
        long sessionsToday = scheduleRepository.findByStartTimeBetweenOrderByStartTimeAsc(dayStart, dayStart.plusDays(1)).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED).count();
        return new AdminDashboard(
                memberRepository.count(),
                memberRepository.countByStatus(MemberStatus.ACTIVE),
                memberRepository.countByJoinedAtBetween(month.atDay(1), month.atEndOfMonth()),
                subscriptionRepository.countMembersWithEffective(today),
                expiring.size(),
                revenueOf(month),
                revenueOf(month.minusMonths(1)),
                trainerRepository.countByActiveTrue(),
                sessionsToday,
                revenue,
                byPlan.entrySet().stream().map(e -> new LabelValue(e.getKey(), e.getValue())).toList(),
                expiring.stream().limit(8).map(mappers::subscription).toList(),
                auditRepository.findTop8ByOrderByCreatedAtDesc().stream().map(mappers::audit).toList());
    }

    public TrainerDashboard trainer(AuthUser user) {
        Trainer t = access.requireTrainerOf(user);
        LocalDate today = mappers.today();
        LocalDate ws = ProgressService.weekStart(today);
        List<WorkoutSchedule> week = scheduleRepository.findInRange(ws.atStartOfDay(), ws.plusDays(7).atStartOfDay(), null, t.getId())
                .stream().filter(s -> s.getStatus() != ScheduleStatus.CANCELLED).toList();
        List<WorkoutSchedule> todayList = week.stream().filter(s -> s.getStartTime().toLocalDate().equals(today)).toList();
        long completed = week.stream().filter(s -> s.getStatus() == ScheduleStatus.COMPLETED).count();
        long missed = week.stream().filter(s -> s.getStatus() == ScheduleStatus.MISSED).count();
        Double rate = completed + missed == 0 ? null : Math.round(completed * 1000.0 / (completed + missed)) / 10.0;
        String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        List<LabelValue> load = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = ws.plusDays(i);
            load.add(new LabelValue(days[i], week.stream().filter(s -> s.getStartTime().toLocalDate().equals(d)).count()));
        }
        List<Member> members = memberRepository.findByTrainerId(t.getId());
        Set<Long> memberIds = new HashSet<>();
        members.forEach(m -> memberIds.add(m.getId()));
        List<SubscriptionResponse> expiring = subscriptionRepository.findEndingBetween(today, today.plusDays(MemberSubscription.EXPIRING_DAYS))
                .stream().filter(s -> memberIds.contains(s.getMember().getId())).map(mappers::subscription).toList();
        long activePlans = planRepository.findByMemberTrainerIdOrderByCreatedAtDesc(t.getId()).stream()
                .filter(p -> p.getStatus() == PlanStatus.ACTIVE).count();
        return new TrainerDashboard(members.size(), todayList.size(), week.size(), completed, missed, rate, activePlans,
                mappers.schedules(todayList), expiring, load);
    }

    public MemberDashboard member(AuthUser user) {
        Member m = access.requireMemberOf(user);
        LocalDate today = mappers.today();
        LocalDateTime now = LocalDateTime.now(clock);
        MemberSubscription current = mappers.pickCurrent(subscriptionRepository.findByMemberIdOrderByStartDateDesc(m.getId()), today);
        LocalDate ws = ProgressService.weekStart(today);
        List<WorkoutSchedule> all = scheduleRepository.findByMemberIdOrderByStartTimeAsc(m.getId());
        List<WorkoutSchedule> week = all.stream().filter(s -> !s.getStartTime().toLocalDate().isBefore(ws)
                && s.getStartTime().toLocalDate().isBefore(ws.plusDays(7)) && s.getStatus() != ScheduleStatus.CANCELLED).toList();
        ScheduleResponse next = all.stream().filter(s -> s.getStatus() == ScheduleStatus.SCHEDULED && s.getEndTime().isAfter(now))
                .findFirst().map(mappers::schedule).orElse(null);
        YearMonth month = YearMonth.from(today);
        long doneMonth = all.stream().filter(s -> s.getStatus() == ScheduleStatus.COMPLETED
                && YearMonth.from(s.getStartTime()).equals(month)).count();
        WeightRecord latest = weightRepository.findFirstByMemberIdOrderByRecordedDateDesc(m.getId()).orElse(null);
        Double bmi = ProgressService.bmi(latest == null ? null : latest.getWeightKg(), m.getHeightCm());
        WorkoutPlanSummary plan = planRepository.findFirstByMemberIdAndStatusOrderByStartDateDesc(m.getId(), PlanStatus.ACTIVE)
                .map(mappers::planSummary).orElse(null);
        return new MemberDashboard(mappers.member(m), current == null ? null : mappers.subscription(current), next,
                mappers.schedules(week), doneMonth, ProgressService.streakWeeks(all, today), mappers.weight(latest),
                bmi, ProgressService.bmiCategory(bmi), plan);
    }

    private long revenueOf(YearMonth ym) {
        Long v = subscriptionRepository.revenueBetween(ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        return v == null ? 0 : v;
    }
}
