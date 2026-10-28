package com.gymflow.repository;

import com.gymflow.domain.ContentSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentSourceRepository extends JpaRepository<ContentSource, Long> {
    List<ContentSource> findAllByOrderByPublisherAscTitleAsc();
}
