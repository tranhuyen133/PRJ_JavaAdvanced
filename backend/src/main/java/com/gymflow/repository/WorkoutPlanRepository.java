package com.gymflow.repository;

import com.gymflow.domain.PlanStatus;
import com.gymflow.domain.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    List<WorkoutPlan> findByMemberIdOrderByStartDateDesc(Long memberId);

    List<WorkoutPlan> findByMemberTrainerIdOrderByCreatedAtDesc(Long trainerId);

    List<WorkoutPlan> findAllByOrderByCreatedAtDesc();

    Optional<WorkoutPlan> findFirstByMemberIdAndStatusOrderByStartDateDesc(Long memberId, PlanStatus status);
}
