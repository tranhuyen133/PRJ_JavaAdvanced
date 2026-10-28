package com.gymflow.repository;

import com.gymflow.domain.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    List<WeightRecord> findByMemberIdOrderByRecordedDateAsc(Long memberId);

    Optional<WeightRecord> findByMemberIdAndRecordedDate(Long memberId, LocalDate recordedDate);

    Optional<WeightRecord> findFirstByMemberIdOrderByRecordedDateDesc(Long memberId);
}
