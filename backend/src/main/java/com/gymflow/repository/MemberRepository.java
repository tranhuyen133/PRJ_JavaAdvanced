package com.gymflow.repository;

import com.gymflow.domain.Member;
import com.gymflow.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserId(Long userId);

    List<Member> findByTrainerId(Long trainerId);

    long countByTrainerId(Long trainerId);

    long countByStatus(MemberStatus status);

    long countByJoinedAtBetween(LocalDate from, LocalDate to);

    boolean existsByCode(String code);

    @Query("select coalesce(max(m.id), 0) from Member m")
    Long maxId();

    @Query(value = """
            select m from Member m join m.user u left join m.trainer t
            where (:q = '' or lower(u.fullName) like lower(concat('%', :q, '%'))
                           or lower(u.email) like lower(concat('%', :q, '%'))
                           or lower(coalesce(u.phone, '')) like lower(concat('%', :q, '%'))
                           or lower(m.code) like lower(concat('%', :q, '%')))
              and (:status is null or m.status = :status)
              and (:trainerId is null or t.id = :trainerId)
            order by m.id desc
            """,
            countQuery = """
            select count(m) from Member m join m.user u left join m.trainer t
            where (:q = '' or lower(u.fullName) like lower(concat('%', :q, '%'))
                           or lower(u.email) like lower(concat('%', :q, '%'))
                           or lower(coalesce(u.phone, '')) like lower(concat('%', :q, '%'))
                           or lower(m.code) like lower(concat('%', :q, '%')))
              and (:status is null or m.status = :status)
              and (:trainerId is null or t.id = :trainerId)
            """)
    Page<Member> search(@Param("q") String q, @Param("status") MemberStatus status,
                        @Param("trainerId") Long trainerId, Pageable pageable);
}
