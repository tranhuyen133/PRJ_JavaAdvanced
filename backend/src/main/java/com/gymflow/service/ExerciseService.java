package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.ContentSourceRepository;
import com.gymflow.repository.ExerciseRepository;
import com.gymflow.repository.UserRepository;
import com.gymflow.repository.WorkoutPlanItemRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ContentSourceRepository sourceRepository;
    private final WorkoutPlanItemRepository planItemRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Mappers mappers;

    /** Bộ lọc danh sách; mọi trường đều tuỳ chọn. */
    public record Filter(String q, MuscleGroup group, Level level, ExerciseLocation location, ExerciseGoal goal,
                         ReviewStatus reviewStatus, boolean includeHidden) {
    }

    /**
     * Hội viên không thấy bài đã ẩn; HLV/quản lý xem được khi bật includeHidden.
     * Bài chưa duyệt vẫn hiện cho mọi vai trò (giáo án cũ đang dùng) nhưng giao diện gắn nhãn "Chờ duyệt".
     */
    @Transactional(readOnly = true)
    public List<ExerciseResponse> list(AuthUser user, Filter f) {
        String needle = f.q() == null ? "" : f.q().trim().toLowerCase(Locale.ROOT);
        boolean showHidden = f.includeHidden() && !user.isMember();
        return exerciseRepository.findAllByOrderByNameAsc().stream()
                .filter(e -> showHidden || !e.isHidden())
                .filter(e -> f.group() == null || e.getMuscleGroup() == f.group())
                .filter(e -> f.level() == null || e.getLevel() == f.level())
                .filter(e -> f.location() == null || e.getLocation() == f.location()
                        || (e.getLocation() == ExerciseLocation.ANY && f.location() != ExerciseLocation.ANY))
                .filter(e -> f.goal() == null || Mappers.parseGoals(e.getGoals()).contains(f.goal()))
                .filter(e -> f.reviewStatus() == null || e.getReviewStatus() == f.reviewStatus())
                .filter(e -> needle.isEmpty() || matches(e, needle))
                .map(mappers::exercise).toList();
    }

    private static boolean matches(Exercise e, String needle) {
        for (String field : new String[]{e.getName(), e.getNameEn(), e.getAliases(), e.getEquipment()}) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(needle)) return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(AuthUser user, Long id) {
        Exercise e = load(id);
        if (e.isHidden() && user.isMember()) throw ApiException.notFound("bài tập");
        return mappers.exercise(e);
    }

    @Transactional(readOnly = true)
    public List<SourceResponse> sources() {
        return sourceRepository.findAllByOrderByPublisherAscTitleAsc().stream().map(mappers::source).toList();
    }

    @Transactional
    public ExerciseResponse create(AuthUser user, ExerciseRequest req) {
        if (exerciseRepository.existsByNameIgnoreCase(req.name().trim())) throw ApiException.conflict("Tên bài tập đã tồn tại");
        Exercise e = new Exercise();
        apply(e, req);
        e.setReviewStatus(ReviewStatus.PENDING);
        e.setCreatedBy(userRepository.getReferenceById(user.id()));
        exerciseRepository.save(e);
        auditService.log("EXERCISE_CREATE", "EXERCISE", e.getId(), e.getName());
        return mappers.exercise(e);
    }

    /** HLV sửa nội dung → bài quay về "Chờ duyệt"; quản lý sửa thì giữ nguyên trạng thái. */
    @Transactional
    public ExerciseResponse update(AuthUser user, Long id, ExerciseRequest req) {
        Exercise e = load(id);
        if (exerciseRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) throw ApiException.conflict("Tên bài tập đã tồn tại");
        apply(e, req);
        if (!user.isAdmin() && e.getReviewStatus() != ReviewStatus.PENDING) {
            e.setReviewStatus(ReviewStatus.PENDING);
            e.setReviewedBy(null);
            e.setReviewedAt(null);
        }
        auditService.log("EXERCISE_UPDATE", "EXERCISE", e.getId(), e.getName());
        return mappers.exercise(e);
    }

    @Transactional
    public ExerciseResponse review(AuthUser user, Long id, ReviewRequest req) {
        Exercise e = load(id);
        String note = Mappers.blankToNull(req.note());
        if (req.status() == ReviewStatus.APPROVED && e.getSources().isEmpty()) {
            throw ApiException.badRequest("Bài tập cần ít nhất một nguồn tham khảo trước khi duyệt");
        }
        if (req.status() == ReviewStatus.NEEDS_REVIEW && note == null) {
            throw ApiException.badRequest("Vui lòng ghi rõ nội dung cần chỉnh sửa");
        }
        e.setReviewStatus(req.status());
        e.setReviewNote(note);
        e.setReviewedBy(userRepository.getReferenceById(user.id()));
        e.setReviewedAt(LocalDateTime.now());
        auditService.log("EXERCISE_REVIEW", "EXERCISE", e.getId(), e.getName() + " → " + req.status());
        return mappers.exercise(e);
    }

    @Transactional
    public ExerciseResponse setHidden(Long id, boolean hidden) {
        Exercise e = load(id);
        e.setHidden(hidden);
        auditService.log(hidden ? "EXERCISE_HIDE" : "EXERCISE_SHOW", "EXERCISE", e.getId(), e.getName());
        return mappers.exercise(e);
    }

    /** Bài đã nằm trong giáo án chỉ được ẩn, không xoá (giữ nguyên giáo án cũ). */
    @Transactional
    public void delete(Long id) {
        Exercise e = load(id);
        if (planItemRepository.existsByExerciseId(id)) {
            throw ApiException.conflict("Bài tập đang được dùng trong giáo án, hãy ẩn thay vì xoá");
        }
        exerciseRepository.delete(e);
        auditService.log("EXERCISE_DELETE", "EXERCISE", id, e.getName());
    }

    private void apply(Exercise e, ExerciseRequest req) {
        e.setName(req.name().trim());
        e.setMuscleGroup(req.muscleGroup());
        e.setEquipment(Mappers.blankToNull(req.equipment()));
        e.setLevel(req.level());
        e.setDescription(Mappers.blankToNull(req.description()));
        e.setInstructions(Mappers.blankToNull(req.instructions()));
        e.setVideoUrl(Mappers.blankToNull(req.videoUrl()));
        e.setNameEn(Mappers.blankToNull(req.nameEn()));
        e.setAliases(Mappers.blankToNull(req.aliases()));
        e.setSecondaryMuscles(Mappers.blankToNull(req.secondaryMuscles()));
        e.setLocation(req.location());
        e.setGoals(Mappers.joinGoals(req.goals()));
        e.setBreathing(Mappers.blankToNull(req.breathing()));
        e.setCommonMistakes(Mappers.blankToNull(req.commonMistakes()));
        e.setSafetyNotes(Mappers.blankToNull(req.safetyNotes()));
        e.setVariations(Mappers.blankToNull(req.variations()));
        e.setSuggestedDose(Mappers.blankToNull(req.suggestedDose()));
        if (req.contentOrigin() != null) e.setContentOrigin(req.contentOrigin());
        if (req.sourceIds() != null) e.setSources(resolveSources(sourceRepository, req.sourceIds()));
    }

    static LinkedHashSet<ContentSource> resolveSources(ContentSourceRepository repo, List<Long> ids) {
        List<Long> distinct = ids.stream().distinct().toList();
        List<ContentSource> found = repo.findAllById(distinct);
        if (found.size() != distinct.size()) throw ApiException.badRequest("Có nguồn tham khảo không tồn tại");
        return new LinkedHashSet<>(found);
    }

    Exercise load(Long id) {
        return exerciseRepository.findById(id).orElseThrow(() -> ApiException.notFound("bài tập"));
    }
}
