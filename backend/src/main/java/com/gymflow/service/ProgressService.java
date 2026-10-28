package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.WeightRecordRepository;
import com.gymflow.repository.WorkoutLogRepository;
import com.gymflow.repository.WorkoutScheduleRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Cân nặng (mỗi ngày một bản ghi, ghi lại thì cập nhật) và tổng hợp tiến độ. */
@Service
@RequiredArgsConstructor
public class ProgressService {

    static final int ATTENDANCE_WEEKS = 8;

    private final WeightRecordRepository weightRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final WorkoutLogRepository logRepository;
    private final AccessService access;
    private final AuditService auditService;
    private final Mappers mappers;

    @Transactional(readOnly = true)
    public List<WeightResponse> weights(AuthUser user, Long memberId) {
        access.requireView(user, memberId);
        return weightRepository.findByMemberIdOrderByRecordedDateAsc(memberId).stream().map(mappers::weight).toList();
    }

    /** Hội viên tự ghi của mình; HLV phụ trách / quản lý ghi hộ. */
    @Transactional
    public WeightResponse upsertWeight(AuthUser user, Long memberId, WeightRequest req) {
        Member m = access.requireView(user, memberId);
        if (req.recordedDate().isAfter(mappers.today())) throw ApiException.badRequest("Không ghi cân nặng cho ngày tương lai");
        WeightRecord w = weightRepository.findByMemberIdAndRecordedDate(memberId, req.recordedDate()).orElseGet(() -> {
            WeightRecord n = new WeightRecord();
            n.setMember(m);
            n.setRecordedDate(req.recordedDate());
            return n;
        });
        boolean isNew = w.getId() == null;
        w.setWeightKg(round1(req.weightKg()));
        w.setBodyFat(req.bodyFat() == null ? null : round1(req.bodyFat()));
        w.setNote(Mappers.blankToNull(req.note()));
        weightRepository.save(w);
        auditService.log(isNew ? "WEIGHT_CREATE" : "WEIGHT_UPDATE", "MEMBER", memberId,
                req.recordedDate() + ": " + w.getWeightKg() + " kg");
        return mappers.weight(w);
    }

    @Transactional
    public void deleteWeight(AuthUser user, Long weightId) {
        WeightRecord w = weightRepository.findById(weightId).orElseThrow(() -> ApiException.notFound("bản ghi cân nặng"));
        if (!access.canView(user, w.getMember())) throw ApiException.forbidden();
        weightRepository.delete(w);
        auditService.log("WEIGHT_DELETE", "MEMBER", w.getMember().getId(), String.valueOf(w.getRecordedDate()));
    }

    @Transactional(readOnly = true)
    public ProgressResponse progress(AuthUser user, Long memberId) {
        Member m = access.requireView(user, memberId);
        return build(m);
    }

    ProgressResponse build(Member m) {
        LocalDate today = mappers.today();
        List<WeightRecord> weights = weightRepository.findByMemberIdOrderByRecordedDateAsc(m.getId());
        Double current = weights.isEmpty() ? null : weights.get(weights.size() - 1).getWeightKg();
        Double start = weights.isEmpty() ? null : weights.get(0).getWeightKg();
        Double change = current == null ? null : round1(current - start);
        Double bodyFat = null;
        for (int i = weights.size() - 1; i >= 0 && bodyFat == null; i--) bodyFat = weights.get(i).getBodyFat();
        Double bmi = bmi(current, m.getHeightCm());

        List<WorkoutSchedule> sessions = scheduleRepository.findByMemberIdOrderByStartTimeAsc(m.getId());
        List<WeekAttendance> attendance = attendance(sessions, today);
        long done = attendance.stream().mapToLong(WeekAttendance::completed).sum();
        long missed = attendance.stream().mapToLong(WeekAttendance::missed).sum();
        Double rate = done + missed == 0 ? null : round1(done * 100.0 / (done + missed));

        List<WorkoutLog> logs = logRepository.findByMemberIdOrderByCompletedAtDesc(m.getId());
        long minutes = logs.stream().map(WorkoutLog::getDurationMinutes).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long calories = logs.stream().map(WorkoutLog::getCalories).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        Double avgRating = logs.stream().map(WorkoutLog::getRating).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().mapToObj(ProgressService::round1).findFirst().orElse(null);

        return new ProgressResponse(m.getId(), m.getHeightCm(), current, start, change, bodyFat, bmi, bmiCategory(bmi),
                weights.stream().map(mappers::weight).toList(), attendance, rate, streakWeeks(sessions, today),
                logs.size(), minutes, calories, avgRating);
    }

    // ---------- Các phép tính thuần (có unit test) ----------

    public static Double bmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null || heightCm <= 0) return null;
        double h = heightCm / 100.0;
        return round1(weightKg / (h * h));
    }

    /** Ngưỡng WHO cho người châu Á (IDI & WPRO 2000). */
    public static String bmiCategory(Double bmi) {
        if (bmi == null) return null;
        if (bmi < 18.5) return "Thiếu cân";
        if (bmi < 23) return "Bình thường";
        if (bmi < 25) return "Thừa cân";
        if (bmi < 30) return "Béo phì độ I";
        return "Béo phì độ II";
    }

    public static LocalDate weekStart(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 8 tuần gần nhất (tuần hiện tại ở cuối). */
    public static List<WeekAttendance> attendance(List<WorkoutSchedule> sessions, LocalDate today) {
        LocalDate thisWeek = weekStart(today);
        List<WeekAttendance> result = new ArrayList<>();
        for (int i = ATTENDANCE_WEEKS - 1; i >= 0; i--) {
            LocalDate ws = thisWeek.minusWeeks(i);
            LocalDate we = ws.plusDays(7);
            long scheduled = 0, completed = 0, missed = 0;
            for (WorkoutSchedule s : sessions) {
                LocalDate d = s.getStartTime().toLocalDate();
                if (d.isBefore(ws) || !d.isBefore(we) || s.getStatus() == ScheduleStatus.CANCELLED) continue;
                scheduled++;
                if (s.getStatus() == ScheduleStatus.COMPLETED) completed++;
                if (s.getStatus() == ScheduleStatus.MISSED) missed++;
            }
            result.add(new WeekAttendance(ws, scheduled, completed, missed));
        }
        return result;
    }

    /**
     * Số tuần liên tiếp có ít nhất 1 buổi hoàn thành, tính lùi từ tuần này.
     * Tuần hiện tại chưa có buổi nào thì không làm đứt chuỗi (tuần chưa kết thúc).
     */
    public static int streakWeeks(List<WorkoutSchedule> sessions, LocalDate today) {
        java.util.Set<LocalDate> weeks = new java.util.HashSet<>();
        for (WorkoutSchedule s : sessions) {
            if (s.getStatus() == ScheduleStatus.COMPLETED) weeks.add(weekStart(s.getStartTime().toLocalDate()));
        }
        LocalDate cursor = weekStart(today);
        if (!weeks.contains(cursor)) cursor = cursor.minusWeeks(1);
        int streak = 0;
        while (weeks.contains(cursor)) {
            streak++;
            cursor = cursor.minusWeeks(1);
        }
        return streak;
    }

    static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
