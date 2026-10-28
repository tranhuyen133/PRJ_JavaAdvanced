package com.gymflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_en", length = 120)
    private String nameEn;

    /** Tên gọi khác, cách nhau bởi dấu phẩy – dùng cho tìm kiếm. */
    private String aliases;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false)
    private MuscleGroup muscleGroup;

    @Column(name = "secondary_muscles")
    private String secondaryMuscles;

    private String equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level = Level.BEGINNER;

    @Enumerated(EnumType.STRING)
    private ExerciseLocation location;

    /** Danh sách {@link ExerciseGoal} cách nhau bởi dấu phẩy. */
    private String goals;

    private String description;

    private String instructions;

    private String breathing;

    @Column(name = "common_mistakes")
    private String commonMistakes;

    @Column(name = "safety_notes")
    private String safetyNotes;

    private String variations;

    @Column(name = "suggested_dose")
    private String suggestedDose;

    @Column(name = "video_url")
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_origin", nullable = false)
    private ContentOrigin contentOrigin = ContentOrigin.SELF_AUTHORED;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "review_note")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToMany
    @JoinTable(name = "exercise_sources",
            joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id"))
    private Set<ContentSource> sources = new LinkedHashSet<>();

    /** Bài tập đã nằm trong kế hoạch thì chỉ ẩn, không xoá cứng. */
    @Column(nullable = false)
    private boolean hidden = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
