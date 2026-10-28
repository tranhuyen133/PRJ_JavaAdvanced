package com.gymflow.repository;

import com.gymflow.domain.Role;
import com.gymflow.domain.User;
import com.gymflow.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByRole(Role role);

    @Query("""
            select u from User u
            where (:q = '' or lower(u.fullName) like lower(concat('%', :q, '%'))
                           or lower(u.email) like lower(concat('%', :q, '%')))
              and (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
            order by u.createdAt desc
            """)
    Page<User> search(@Param("q") String q, @Param("role") Role role, @Param("status") UserStatus status, Pageable pageable);
}
