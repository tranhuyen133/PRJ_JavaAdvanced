package com.gymflow.repository;

import com.gymflow.domain.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {
    Optional<Trainer> findByUserId(Long userId);

    List<Trainer> findAllByOrderByIdAsc();

    long countByActiveTrue();
}
