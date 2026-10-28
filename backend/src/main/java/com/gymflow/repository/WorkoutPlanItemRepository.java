package com.gymflow.repository;

import com.gymflow.domain.WorkoutPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, Long> {
    boolean existsByExerciseId(Long exerciseId);

    long countByExerciseId(Long exerciseId);
}
