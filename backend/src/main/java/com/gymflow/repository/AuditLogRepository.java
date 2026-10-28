package com.gymflow.repository;

import com.gymflow.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:q = '' or lower(coalesce(a.actorEmail, '')) like lower(concat('%', :q, '%'))
                           or lower(coalesce(a.detail, '')) like lower(concat('%', :q, '%')))
              and (:action = '' or a.action = :action)
              and a.createdAt >= :from and a.createdAt < :to
            order by a.createdAt desc
            """)
    Page<AuditLog> search(@Param("q") String q, @Param("action") String action,
                          @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select distinct a.action from AuditLog a order by a.action")
    List<String> distinctActions();

    List<AuditLog> findTop8ByOrderByCreatedAtDesc();
}
