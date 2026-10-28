package com.gymflow.service;

import com.gymflow.domain.ScheduleStatus;
import com.gymflow.domain.WorkoutSchedule;
import com.gymflow.dto.Dtos.WeekAttendance;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28); // thứ Tư

    private static WorkoutSchedule session(LocalDate day, ScheduleStatus status) {
        WorkoutSchedule s = new WorkoutSchedule();
        s.setStartTime(day.atTime(18, 0));
        s.setEndTime(day.atTime(19, 0));
        s.setStatus(status);
        return s;
    }

    @Test
    void bmiUsesAsianThresholds() {
        assertThat(ProgressService.bmi(65.0, 170.0)).isEqualTo(22.5);
        assertThat(ProgressService.bmiCategory(18.4)).isEqualTo("Thiếu cân");
        assertThat(ProgressService.bmiCategory(22.9)).isEqualTo("Bình thường");
        assertThat(ProgressService.bmiCategory(23.0)).isEqualTo("Thừa cân");
        assertThat(ProgressService.bmiCategory(25.0)).isEqualTo("Béo phì độ I");
        assertThat(ProgressService.bmiCategory(30.0)).isEqualTo("Béo phì độ II");
        assertThat(ProgressService.bmi(null, 170.0)).isNull();
        assertThat(ProgressService.bmi(65.0, null)).isNull();
    }

    @Test
    void weekStartsOnMonday() {
        assertThat(ProgressService.weekStart(TODAY)).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(ProgressService.weekStart(LocalDate.of(2026, 11, 1))).isEqualTo(LocalDate.of(2026, 10, 26)); // CN
    }

    @Test
    void attendanceReturnsEightWeeksAndIgnoresCancelled() {
        List<WorkoutSchedule> list = List.of(
                session(TODAY.minusDays(1), ScheduleStatus.COMPLETED),
                session(TODAY.minusDays(2), ScheduleStatus.CANCELLED),
                session(TODAY.minusWeeks(1), ScheduleStatus.MISSED),
                session(TODAY.minusWeeks(20), ScheduleStatus.COMPLETED));
        List<WeekAttendance> weeks = ProgressService.attendance(list, TODAY);
        assertThat(weeks).hasSize(8);
        WeekAttendance current = weeks.get(7);
        assertThat(current.weekStart()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(current.scheduled()).isEqualTo(1);
        assertThat(current.completed()).isEqualTo(1);
        assertThat(weeks.get(6).missed()).isEqualTo(1);
        assertThat(weeks.stream().mapToLong(WeekAttendance::completed).sum()).isEqualTo(1);
    }

    @Test
    void streakCountsConsecutiveWeeksAndToleratesEmptyCurrentWeek() {
        List<WorkoutSchedule> list = new ArrayList<>();
        for (int w = 1; w <= 3; w++) list.add(session(TODAY.minusWeeks(w), ScheduleStatus.COMPLETED));
        list.add(session(TODAY.minusWeeks(5), ScheduleStatus.COMPLETED)); // đứt ở tuần -4
        assertThat(ProgressService.streakWeeks(list, TODAY)).isEqualTo(3);

        list.add(session(TODAY.minusDays(1), ScheduleStatus.COMPLETED));
        assertThat(ProgressService.streakWeeks(list, TODAY)).isEqualTo(4);
    }

    @Test
    void missedSessionsDoNotCountForStreak() {
        List<WorkoutSchedule> list = List.of(session(TODAY.minusWeeks(1), ScheduleStatus.MISSED));
        assertThat(ProgressService.streakWeeks(list, TODAY)).isZero();
    }
}
