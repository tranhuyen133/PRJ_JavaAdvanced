package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.MemberSubscriptionRepository;
import com.gymflow.repository.WorkoutScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;

    public ReportResponse summary(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw ApiException.badRequest("Khoảng ngày không hợp lệ");
        if (ChronoUnit.DAYS.between(from, to) > 366) throw ApiException.badRequest("Báo cáo tối đa 1 năm");

        // Doanh thu ghi nhận theo ngày tạo đăng ký (thời điểm thu tiền).
        List<MemberSubscription> subs = subscriptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(s -> !s.isCancelled())
                .filter(s -> !s.getCreatedAt().toLocalDate().isBefore(from) && !s.getCreatedAt().toLocalDate().isAfter(to))
                .toList();
        long revenue = subs.stream().mapToLong(MemberSubscription::getPricePaid).sum();
        Map<YearMonth, Long> byMonth = new TreeMap<>();
        for (YearMonth ym = YearMonth.from(from); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) byMonth.put(ym, 0L);
        Map<String, Long> byPlan = new TreeMap<>();
        for (MemberSubscription s : subs) {
            byMonth.merge(YearMonth.from(s.getCreatedAt()), s.getPricePaid(), Long::sum);
            byPlan.merge(s.getPlan().getName(), s.getPricePaid(), Long::sum);
        }

        List<WorkoutSchedule> sessions = scheduleRepository.findByStartTimeBetweenOrderByStartTimeAsc(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        long cancelled = count(sessions, ScheduleStatus.CANCELLED);
        long completed = count(sessions, ScheduleStatus.COMPLETED);
        long missed = count(sessions, ScheduleStatus.MISSED);
        long scheduled = sessions.size() - cancelled;
        Double rate = completed + missed == 0 ? null : Math.round(completed * 1000.0 / (completed + missed)) / 10.0;

        Map<Long, long[]> perTrainer = new LinkedHashMap<>();
        Map<Long, String> trainerNames = new HashMap<>();
        for (WorkoutSchedule s : sessions) {
            if (s.getTrainer() == null || s.getStatus() == ScheduleStatus.CANCELLED) continue;
            long[] c = perTrainer.computeIfAbsent(s.getTrainer().getId(), k -> new long[3]);
            trainerNames.putIfAbsent(s.getTrainer().getId(), s.getTrainer().getUser().getFullName());
            c[0]++;
            if (s.getStatus() == ScheduleStatus.COMPLETED) c[1]++;
            if (s.getStatus() == ScheduleStatus.MISSED) c[2]++;
        }
        List<TrainerAttendance> byTrainer = perTrainer.entrySet().stream()
                .map(e -> new TrainerAttendance(e.getKey(), trainerNames.get(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted(Comparator.comparingLong(TrainerAttendance::total).reversed()).toList();

        List<Member> members = memberRepository.findAll();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (MemberStatus st : MemberStatus.values()) byStatus.put(st.name(), 0L);
        Map<String, Long> byGender = new LinkedHashMap<>();
        Map<YearMonth, Long> newByMonth = new TreeMap<>();
        for (YearMonth ym : byMonth.keySet()) newByMonth.put(ym, 0L);
        for (Member m : members) {
            byStatus.merge(m.getStatus().name(), 1L, Long::sum);
            byGender.merge(m.getGender() == null ? "UNKNOWN" : m.getGender().name(), 1L, Long::sum);
            if (m.getJoinedAt() != null && !m.getJoinedAt().isBefore(from) && !m.getJoinedAt().isAfter(to)) {
                newByMonth.merge(YearMonth.from(m.getJoinedAt()), 1L, Long::sum);
            }
        }

        return new ReportResponse(from, to, revenue, subs.size(), monthList(byMonth), list(byPlan),
                scheduled, completed, missed, cancelled, rate, byTrainer, list(byStatus), list(byGender), monthList(newByMonth));
    }

    private static long count(List<WorkoutSchedule> list, ScheduleStatus st) {
        return list.stream().filter(s -> s.getStatus() == st).count();
    }

    private static List<LabelValue> list(Map<String, Long> map) {
        return map.entrySet().stream().map(e -> new LabelValue(e.getKey(), e.getValue())).toList();
    }

    private static List<LabelValue> monthList(Map<YearMonth, Long> map) {
        return map.entrySet().stream()
                .map(e -> new LabelValue(String.format("%02d/%d", e.getKey().getMonthValue(), e.getKey().getYear()), e.getValue()))
                .toList();
    }
}
