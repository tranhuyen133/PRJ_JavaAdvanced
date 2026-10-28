package com.gymflow.web;

import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.ExerciseService;
import com.gymflow.service.WorkoutPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bài tập & giáo án")
@RestController
@RequiredArgsConstructor
public class TrainingController {

    private final ExerciseService exerciseService;
    private final WorkoutPlanService planService;

    // ---------- Thư viện bài tập ----------
    @GetMapping("/api/exercises")
    public List<ExerciseResponse> exercises(@RequestParam(defaultValue = "") String q,
                                            @RequestParam(required = false) MuscleGroup muscleGroup,
                                            @RequestParam(required = false) Level level,
                                            @RequestParam(required = false) ExerciseLocation location,
                                            @RequestParam(required = false) ExerciseGoal goal,
                                            @RequestParam(required = false) ReviewStatus reviewStatus,
                                            @RequestParam(defaultValue = "false") boolean includeHidden) {
        return exerciseService.list(CurrentUser.get(),
                new ExerciseService.Filter(q, muscleGroup, level, location, goal, reviewStatus, includeHidden));
    }

    @GetMapping("/api/exercises/{id}")
    public ExerciseResponse exercise(@PathVariable Long id) {
        return exerciseService.get(CurrentUser.get(), id);
    }

    @GetMapping("/api/content-sources")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public List<SourceResponse> contentSources() {
        return exerciseService.sources();
    }

    @PutMapping("/api/exercises/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ExerciseResponse reviewExercise(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        return exerciseService.review(CurrentUser.get(), id, req);
    }

    @PostMapping("/api/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ExerciseResponse createExercise(@Valid @RequestBody ExerciseRequest req) {
        return exerciseService.create(CurrentUser.get(), req);
    }

    @PutMapping("/api/exercises/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ExerciseResponse updateExercise(@PathVariable Long id, @Valid @RequestBody ExerciseRequest req) {
        return exerciseService.update(CurrentUser.get(), id, req);
    }

    @PutMapping("/api/exercises/{id}/hidden")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ExerciseResponse hideExercise(@PathVariable Long id, @Valid @RequestBody HiddenRequest req) {
        return exerciseService.setHidden(id, req.hidden());
    }

    @DeleteMapping("/api/exercises/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteExercise(@PathVariable Long id) {
        exerciseService.delete(id);
    }

    // ---------- Giáo án ----------
    @GetMapping("/api/workout-plans")
    public List<WorkoutPlanSummary> plans(@RequestParam(required = false) Long memberId) {
        return planService.list(CurrentUser.get(), memberId);
    }

    @GetMapping("/api/workout-plans/me/current")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<WorkoutPlanResponse> current() {
        WorkoutPlanResponse p = planService.current(CurrentUser.get());
        return p == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(p);
    }

    @GetMapping("/api/workout-plans/{id}")
    public WorkoutPlanResponse plan(@PathVariable Long id) {
        return planService.get(CurrentUser.get(), id);
    }

    @PostMapping("/api/workout-plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public WorkoutPlanResponse createPlan(@Valid @RequestBody WorkoutPlanRequest req) {
        return planService.create(CurrentUser.get(), req);
    }

    @PutMapping("/api/workout-plans/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public WorkoutPlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody WorkoutPlanRequest req) {
        return planService.update(CurrentUser.get(), id, req);
    }

    @PutMapping("/api/workout-plans/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public WorkoutPlanSummary planStatus(@PathVariable Long id, @Valid @RequestBody PlanStatusRequest req) {
        return planService.setStatus(CurrentUser.get(), id, req.status());
    }

    @DeleteMapping("/api/workout-plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public void deletePlan(@PathVariable Long id) {
        planService.delete(CurrentUser.get(), id);
    }
}
