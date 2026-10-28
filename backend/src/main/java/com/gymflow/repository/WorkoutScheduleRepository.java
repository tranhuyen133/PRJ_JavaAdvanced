package com.gymflow.repository;

import com.gymflow.domain.Member;
import com.gymflow.domain.ScheduleStatus;
import com.gymflow.domain.Trainer;
import com.gymflow.domain.WorkoutSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkoutScheduleRepository extends JpaRepository<WorkoutSchedule, Long> {
    /**
     * Khoá dòng hội viên rồi HLV (luôn theo thứ tự này để tránh deadlock) trước khi kiểm tra trùng giờ.
     * Thay cho ràng buộc EXCLUDE của PostgreSQL mà MySQL không có.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> lockMember(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trainer t where t.id = :trainerId")
    Optional<Trainer> lockTrainer(@Param("trainerId") Long trainerId);

    @Query("""
            select s from WorkoutSchedule s left join s.trainer t
            where s.startTime < :to and s.endTime > :from
              and (:memberId is null or s.member.id = :memberId)
              and (:trainerId is null or t.id = :trainerId)
            order by s.startTime asc
            """)
    List<WorkoutSchedule> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                      @Param("memberId") Long memberId, @Param("trainerId") Long trainerId);

    @Query("""
            select count(s) > 0 from WorkoutSchedule s
            where s.trainer.id = :trainerId and s.status <> :cancelled
              and s.startTime < :end and s.endTime > :start
              and (:excludeId is null or s.id <> :excludeId)
            """)
    boolean trainerBusy(@Param("trainerId") Long trainerId, @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId,
                        @Param("cancelled") ScheduleStatus cancelled);

    @Query("""
            select count(s) > 0 from WorkoutSchedule s
            where s.member.id = :memberId and s.status <> :cancelled
              and s.startTime < :end and s.endTime > :start
              and (:excludeId is null or s.id <> :excludeId)
            """)
    boolean memberBusy(@Param("memberId") Long memberId, @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId,
                       @Param("cancelled") ScheduleStatus cancelled);

    @Modifying
    @Query("update WorkoutSchedule s set s.status = :missed where s.status = :scheduled and s.endTime < :now")
    int markMissed(@Param("now") LocalDateTime now, @Param("scheduled") ScheduleStatus scheduled,
                   @Param("missed") ScheduleStatus missed);

    long countByStatusAndStartTimeBetween(ScheduleStatus status, LocalDateTime from, LocalDateTime to);

    long countByMemberIdAndStatus(Long memberId, ScheduleStatus status);

    long countByMemberIdAndStatusNot(Long memberId, ScheduleStatus status);

    List<WorkoutSchedule> findByMemberIdOrderByStartTimeAsc(Long memberId);

    List<WorkoutSchedule> findByPlanId(Long planId);

    List<WorkoutSchedule> findByMemberIdAndStartTimeBetweenOrderByStartTimeAsc(Long memberId, LocalDateTime from, LocalDateTime to);

    List<WorkoutSchedule> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime from, LocalDateTime to);
}
