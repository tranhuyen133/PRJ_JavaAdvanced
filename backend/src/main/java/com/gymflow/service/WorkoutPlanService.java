package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.WorkoutPlanRepository;
import com.gymflow.repository.WorkoutScheduleRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository planRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final ExerciseService exerciseService;
    private final AccessService access;
    private final AuditService auditService;
    private final Mappers mappers;

    @Transactional(readOnly = true)
    public List<WorkoutPlanSummary> list(AuthUser user, Long memberId) {
        List<WorkoutPlan> plans;
        if (memberId != null) {
            access.requireView(user, memberId);
            plans = planRepository.findByMemberIdOrderByStartDateDesc(memberId);
        } else if (user.isAdmin()) {
            plans = planRepository.findAllByOrderByCreatedAtDesc();
        } else if (user.isTrainer()) {
            plans = planRepository.findByMemberTrainerIdOrderByCreatedAtDesc(access.requireTrainerOf(user).getId());
        } else {
            plans = planRepository.findByMemberIdOrderByStartDateDesc(access.requireMemberOf(user).getId());
        }
        return plans.stream().map(mappers::planSummary).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse get(AuthUser user, Long id) {
        WorkoutPlan p = load(id);
        if (!access.canView(user, p.getMember())) throw ApiException.forbidden();
        return mappers.planDetail(p);
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse current(AuthUser user) {
        Member m = access.requireMemberOf(user);
        return planRepository.findFirstByMemberIdAndStatusOrderByStartDateDesc(m.getId(), PlanStatus.ACTIVE)
                .map(mappers::planDetail).orElse(null);
    }

    @Transactional
    public WorkoutPlanResponse create(AuthUser user, WorkoutPlanRequest req) {
        Member m = access.requireCoach(user, req.memberId());
        validate(req);
        WorkoutPlan p = new WorkoutPlan();
        p.setMember(m);
        p.setTrainer(user.isTrainer() ? access.requireTrainerOf(user) : m.getTrainer());
        p.setStatus(PlanStatus.ACTIVE);
        apply(p, req);
        planRepository.save(p);
        archiveOtherActive(p);
        auditService.log("WORKOUT_PLAN_CREATE", "WORKOUT_PLAN", p.getId(), m.getCode() + " · " + p.getName());
        return mappers.planDetail(p);
    }

    @Transactional
    public WorkoutPlanResponse update(AuthUser user, Long id, WorkoutPlanRequest req) {
        WorkoutPlan p = load(id);
        if (!access.canCoach(user, p.getMember())) throw ApiException.forbidden();
        if (!p.getMember().getId().equals(req.memberId())) throw ApiException.badRequest("Không thể chuyển giáo án sang hội viên khác");
        validate(req);
        apply(p, req);
        planRepository.saveAndFlush(p);
        auditService.log("WORKOUT_PLAN_UPDATE", "WORKOUT_PLAN", p.getId(), p.getName());
        return mappers.planDetail(p);
    }

    @Transactional
    public WorkoutPlanSummary setStatus(AuthUser user, Long id, PlanStatus status) {
        WorkoutPlan p = load(id);
        if (!access.canCoach(user, p.getMember())) throw ApiException.forbidden();
        p.setStatus(status);
        if (status == PlanStatus.ACTIVE) archiveOtherActive(p);
        auditService.log("WORKOUT_PLAN_STATUS", "WORKOUT_PLAN", p.getId(), p.getName() + " → " + status);
        return mappers.planSummary(p);
    }

    @Transactional
    public void delete(AuthUser user, Long id) {
        WorkoutPlan p = load(id);
        if (!access.canCoach(user, p.getMember())) throw ApiException.forbidden();
        // Buổi tập đã gắn giáo án vẫn giữ lại, chỉ bỏ liên kết.
        scheduleRepository.findByPlanId(id).forEach(s -> s.setPlan(null));
        planRepository.delete(p);
        auditService.log("WORKOUT_PLAN_DELETE", "WORKOUT_PLAN", id, p.getName());
    }

    /** Mỗi hội viên chỉ có một giáo án đang áp dụng; giáo án cũ chuyển sang lưu trữ. */
    private void archiveOtherActive(WorkoutPlan current) {
        planRepository.findByMemberIdOrderByStartDateDesc(current.getMember().getId()).stream()
                .filter(o -> !o.getId().equals(current.getId()) && o.getStatus() == PlanStatus.ACTIVE)
                .forEach(o -> o.setStatus(PlanStatus.ARCHIVED));
    }

    private void validate(WorkoutPlanRequest req) {
        if (req.endDate().isBefore(req.startDate())) throw ApiException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (req.startDate().plusYears(1).isBefore(req.endDate())) throw ApiException.badRequest("Giáo án dài tối đa 1 năm");
        if (req.items().isEmpty()) throw ApiException.badRequest("Giáo án cần ít nhất một bài tập");
    }

    private void apply(WorkoutPlan p, WorkoutPlanRequest req) {
        p.setName(req.name().trim());
        p.setGoal(Mappers.blankToNull(req.goal()));
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setNote(Mappers.blankToNull(req.note()));
        p.getItems().clear();
        int order = 0;
        for (PlanItemRequest r : req.items()) {
            WorkoutPlanItem i = new WorkoutPlanItem();
            i.setPlan(p);
            i.setExercise(exerciseService.load(r.exerciseId()));
            i.setDayOfWeek(r.dayOfWeek());
            i.setSortOrder(r.sortOrder() == null ? order : r.sortOrder());
            i.setSets(r.sets());
            i.setReps(r.reps().trim());
            i.setWeightKg(r.weightKg());
            i.setRestSeconds(r.restSeconds());
            i.setDurationMinutes(r.durationMinutes());
            i.setNote(Mappers.blankToNull(r.note()));
            p.getItems().add(i);
            order++;
        }
    }

    WorkoutPlan load(Long id) {
        return planRepository.findById(id).orElseThrow(() -> ApiException.notFound("giáo án"));
    }
}
