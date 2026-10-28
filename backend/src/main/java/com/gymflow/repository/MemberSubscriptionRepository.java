package com.gymflow.repository;

import com.gymflow.domain.Member;
import com.gymflow.domain.MemberSubscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    /**
     * SELECT … FOR UPDATE dòng hội viên: tuần tự hoá các lần đăng ký gói của cùng một hội viên
     * để kiểm tra trùng thời gian (existsOverlap) không bị hai yêu cầu đồng thời vượt qua.
     * Thay cho ràng buộc EXCLUDE của PostgreSQL mà MySQL không có.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> lockMember(@Param("memberId") Long memberId);

    List<MemberSubscription> findByMemberIdOrderByStartDateDesc(Long memberId);

    List<MemberSubscription> findAllByOrderByCreatedAtDesc();

    boolean existsByPlanId(Long planId);

    @Query("""
            select count(s) from MemberSubscription s
            where s.plan.id = :planId and s.cancelled = false and s.endDate >= :today
            """)
    long countCurrentByPlan(@Param("planId") Long planId, @Param("today") LocalDate today);

    @Query("""
            select count(s) > 0 from MemberSubscription s
            where s.member.id = :memberId and s.cancelled = false
              and s.startDate <= :end and s.endDate >= :start
            """)
    boolean existsOverlap(@Param("memberId") Long memberId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select max(s.endDate) from MemberSubscription s where s.member.id = :memberId and s.cancelled = false")
    LocalDate maxEndDate(@Param("memberId") Long memberId);

    @Query("""
            select s from MemberSubscription s
            where s.member.id = :memberId and s.cancelled = false
              and s.startDate <= :day and s.endDate >= :day
            """)
    List<MemberSubscription> findEffective(@Param("memberId") Long memberId, @Param("day") LocalDate day);

    @Query("""
            select count(distinct s.member.id) from MemberSubscription s
            where s.cancelled = false and s.startDate <= :day and s.endDate >= :day
            """)
    long countMembersWithEffective(@Param("day") LocalDate day);

    @Query("""
            select s from MemberSubscription s
            where s.cancelled = false and s.endDate between :from and :to
            order by s.endDate asc
            """)
    List<MemberSubscription> findEndingBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(s.pricePaid), 0) from MemberSubscription s
            where s.cancelled = false and s.createdAt >= :from and s.createdAt < :to
            """)
    Long revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
