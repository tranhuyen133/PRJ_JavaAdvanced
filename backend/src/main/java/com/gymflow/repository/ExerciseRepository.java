package com.gymflow.repository;

import com.gymflow.domain.Exercise;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    /** Nạp sẵn nguồn và người tạo/duyệt để danh sách không phát sinh N+1 truy vấn. */
    @EntityGraph(attributePaths = {"sources", "createdBy", "reviewedBy"})
    List<Exercise> findAllByOrderByNameAsc();

    Optional<Exercise> findFirstByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
