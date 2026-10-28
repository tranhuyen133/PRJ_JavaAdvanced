package com.gymflow.service;

import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Chuyển entity → DTO. Các trường dẫn xuất (trạng thái gói, số buổi…) được tính tại đây, không lưu trong CSDL. */
@Component
@RequiredArgsConstructor
public class Mappers {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final WorkoutPlanItemRepository planItemRepository;
    private final WorkoutLogRepository logRepository;
    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---------- Member ----------
    public MemberResponse member(Member m) {
        LocalDate today = today();
        List<MemberSubscription> subs = subscriptionRepository.findByMemberIdOrderByStartDateDesc(m.getId());
        MemberSubscription current = pickCurrent(subs, today);
        User u = m.getUser();
        Trainer t = m.getTrainer();
        return new MemberResponse(m.getId(), u.getId(), m.getCode(), u.getFullName(), u.getEmail(), u.getPhone(),
                m.getDateOfBirth(), m.getGender(), m.getHeightCm(), m.getGoal(), m.getAddress(), m.getStatus(),
                u.getStatus(), t == null ? null : t.getId(), t == null ? null : t.getUser().getFullName(),
                m.getJoinedAt(), m.getNote(),
                current == null ? null : current.getPlan().getName(),
                current == null ? null : current.stateOn(today),
                current == null ? null : current.getEndDate(),
                current == null ? null : current.daysLeft(today));
    }

    /** Gói "đại diện": đang hiệu lực → sắp tới gần nhất → gói hết hạn gần nhất. */
    public MemberSubscription pickCurrent(List<MemberSubscription> subs, LocalDate today) {
        return subs.stream().filter(s -> s.isEffectiveOn(today)).findFirst()
                .or(() -> subs.stream().filter(s -> !s.isCancelled() && s.getStartDate().isAfter(today))
                        .min(Comparator.comparing(MemberSubscription::getStartDate)))
                .or(() -> subs.stream().filter(s -> !s.isCancelled())
                        .max(Comparator.comparing(MemberSubscription::getEndDate)))
                .orElse(null);
    }

    // ---------- Trainer ----------
    public TrainerResponse trainer(Trainer t) {
        User u = t.getUser();
        List<ShiftResponse> shifts = t.getShifts().stream()
                .sorted(Comparator.comparing(TrainerShift::getDayOfWeek).thenComparing(TrainerShift::getStartTime))
                .map(s -> new ShiftResponse(s.getId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();
        return new TrainerResponse(t.getId(), u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), t.getSpecialty(),
                t.getExperienceYears(), t.getCertificates(), t.getBio(), t.isActive(), u.getStatus(),
                memberRepository.countByTrainerId(t.getId()), shifts);
    }

    // ---------- Plan ----------
    public PlanResponse plan(MembershipPlan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getDurationDays(),
                p.getBenefits(), p.isActive(), subscriptionRepository.countCurrentByPlan(p.getId(), today()));
    }

    // ---------- Subscription ----------
    public SubscriptionResponse subscription(MemberSubscription s) {
        LocalDate today = today();
        Member m = s.getMember();
        return new SubscriptionResponse(s.getId(), m.getId(), m.getUser().getFullName(), m.getCode(),
                s.getPlan().getId(), s.getPlan().getName(), s.getStartDate(), s.getEndDate(), s.getPricePaid(),
                s.stateOn(today), s.daysLeft(today), s.isCancelled(), s.getCancelledAt(), s.getNote(),
                s.getCreatedAt(), s.getCreatedBy() == null ? null : s.getCreatedBy().getFullName());
    }

    // ---------- Exercise ----------
    public ExerciseResponse exercise(Exercise e) {
        return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroup(), e.getEquipment(), e.getLevel(),
                e.getDescription(), e.getInstructions(), e.getVideoUrl(), e.isHidden(),
                planItemRepository.countByExerciseId(e.getId()),
                e.getCreatedBy() == null ? null : e.getCreatedBy().getFullName(),
                e.getNameEn(), e.getAliases(), e.getSecondaryMuscles(), e.getLocation(), parseGoals(e.getGoals()),
                e.getBreathing(), e.getCommonMistakes(), e.getSafetyNotes(), e.getVariations(), e.getSuggestedDose(),
                e.getContentOrigin(), e.getReviewStatus(), e.getReviewNote(),
                e.getReviewedBy() == null ? null : e.getReviewedBy().getFullName(), e.getReviewedAt(), e.getUpdatedAt(),
                sources(e.getSources()));
    }

    // ---------- Nguồn & kho kiến thức ----------
    public SourceResponse source(ContentSource s) {
        return new SourceResponse(s.getId(), s.getCode(), s.getPublisher(), s.getTitle(), s.getUrl(), s.getLicense(),
                s.getLicenseUrl(), s.isLicenseVerified(), s.getAttribution(), s.getPublisherReviewed(), s.getAccessedAt());
    }

    public List<SourceResponse> sources(java.util.Collection<ContentSource> list) {
        return list.stream().sorted(Comparator.comparing(ContentSource::getId)).map(this::source).toList();
    }

    public ArticleSummary articleSummary(Article a) {
        return new ArticleSummary(a.getId(), a.getSlug(), a.getTitle(), a.getCategory(), a.getSummary(), a.getPublishedAt());
    }

    /** internal = false: trang công khai, ẩn ghi chú duyệt và tên người duyệt/tác giả. */
    public ArticleResponse article(Article a, boolean internal) {
        return new ArticleResponse(a.getId(), a.getSlug(), a.getTitle(), a.getCategory(), a.getSummary(), a.getBody(),
                a.getContentOrigin(), a.getStatus(),
                internal ? a.getReviewNote() : null,
                internal && a.getAuthor() != null ? a.getAuthor().getFullName() : null,
                internal && a.getAuthor() != null ? a.getAuthor().getId() : null,
                internal && a.getReviewedBy() != null ? a.getReviewedBy().getFullName() : null,
                a.getReviewedAt(), a.getPublishedAt(),
                internal ? a.getCreatedAt() : null, a.getUpdatedAt(), sources(a.getSources()));
    }

    public static List<ExerciseGoal> parseGoals(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return ExerciseGoal.valueOf(s);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();
    }

    public static String joinGoals(List<ExerciseGoal> goals) {
        if (goals == null || goals.isEmpty()) return null;
        return goals.stream().distinct().map(Enum::name).collect(Collectors.joining(","));
    }

    // ---------- Workout plan ----------
    public WorkoutPlanSummary planSummary(WorkoutPlan p) {
        Trainer t = p.getTrainer();
        int days = (int) p.getItems().stream().map(WorkoutPlanItem::getDayOfWeek).distinct().count();
        return new WorkoutPlanSummary(p.getId(), p.getMember().getId(), p.getMember().getUser().getFullName(),
                t == null ? null : t.getId(), t == null ? null : t.getUser().getFullName(), p.getName(), p.getGoal(),
                p.getStartDate(), p.getEndDate(), p.getStatus(), p.getItems().size(), days, p.getCreatedAt());
    }

    public WorkoutPlanResponse planDetail(WorkoutPlan p) {
        List<PlanItemResponse> items = p.getItems().stream().map(i -> new PlanItemResponse(i.getId(),
                i.getExercise().getId(), i.getExercise().getName(), i.getExercise().getMuscleGroup(),
                i.getExercise().getEquipment(), i.getDayOfWeek(), i.getSortOrder(), i.getSets(), i.getReps(),
                i.getWeightKg(), i.getRestSeconds(), i.getDurationMinutes(), i.getNote())).toList();
        return new WorkoutPlanResponse(planSummary(p), p.getNote(), items);
    }

    // ---------- Schedule ----------
    public ScheduleResponse schedule(WorkoutSchedule s, WorkoutLog log) {
        Trainer t = s.getTrainer();
        WorkoutPlan p = s.getPlan();
        return new ScheduleResponse(s.getId(), s.getMember().getId(), s.getMember().getUser().getFullName(),
                t == null ? null : t.getId(), t == null ? null : t.getUser().getFullName(),
                p == null ? null : p.getId(), p == null ? null : p.getName(), s.getTitle(), s.getStartTime(),
                s.getEndTime(), s.getStatus(), s.getNote(), log == null ? null : log(log));
    }

    public ScheduleResponse schedule(WorkoutSchedule s) {
        WorkoutLog log = s.getStatus() == ScheduleStatus.COMPLETED ? logRepository.findByScheduleId(s.getId()).orElse(null) : null;
        return schedule(s, log);
    }

    /** Map danh sách buổi tập kèm nhật ký, chỉ truy vấn nhật ký một lần cho mỗi hội viên. */
    public List<ScheduleResponse> schedules(List<WorkoutSchedule> list) {
        Map<Long, WorkoutLog> logs = list.stream()
                .filter(s -> s.getStatus() == ScheduleStatus.COMPLETED)
                .map(s -> s.getMember().getId()).distinct()
                .flatMap(mid -> logRepository.findByMemberIdOrderByCompletedAtDesc(mid).stream())
                .collect(Collectors.toMap(l -> l.getSchedule().getId(), Function.identity(), (a, b) -> a));
        return list.stream().map(s -> schedule(s, logs.get(s.getId()))).toList();
    }

    public WorkoutLogResponse log(WorkoutLog l) {
        return new WorkoutLogResponse(l.getId(), l.getCompletedAt(), l.getDurationMinutes(), l.getCalories(),
                l.getRating(), l.getNote());
    }

    // ---------- Others ----------
    public WeightResponse weight(WeightRecord w) {
        return w == null ? null : new WeightResponse(w.getId(), w.getRecordedDate(), w.getWeightKg(), w.getBodyFat(), w.getNote());
    }

    public UserResponse user(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), u.getStatus(),
                u.getLastLoginAt(), u.getCreatedAt());
    }

    public AuditResponse audit(AuditLog a) {
        return new AuditResponse(a.getId(), a.getActorId(), a.getActorEmail(), a.getAction(), a.getTargetType(),
                a.getTargetId(), a.getDetail(), a.isSuccess(), a.getIp(), a.getCreatedAt());
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static boolean same(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
