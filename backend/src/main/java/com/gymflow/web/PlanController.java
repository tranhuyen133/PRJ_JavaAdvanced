package com.gymflow.web;

import com.gymflow.domain.SubscriptionState;
import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.PlanService;
import com.gymflow.service.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Gói tập & đăng ký")
@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final SubscriptionService subscriptionService;

    // ---------- Gói tập ----------
    @GetMapping("/api/plans/public")
    public List<PlanResponse> publicPlans() {
        return planService.list(false);
    }

    @GetMapping("/api/plans")
    public List<PlanResponse> plans(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return planService.list(includeInactive && CurrentUser.get().isAdmin());
    }

    @PostMapping("/api/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PlanResponse create(@Valid @RequestBody PlanRequest req) {
        return planService.create(req);
    }

    @PutMapping("/api/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return planService.update(id, req);
    }

    @PutMapping("/api/plans/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public PlanResponse active(@PathVariable Long id, @Valid @RequestBody ActiveRequest req) {
        return planService.setActive(id, req.active());
    }

    @DeleteMapping("/api/plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        planService.delete(id);
    }

    // ---------- Đăng ký gói ----------
    @GetMapping("/api/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SubscriptionResponse> subscriptions(@RequestParam(required = false) SubscriptionState state,
                                                    @RequestParam(required = false) Long memberId) {
        return subscriptionService.list(state, memberId);
    }

    @GetMapping("/api/subscriptions/me")
    @PreAuthorize("hasRole('MEMBER')")
    public List<SubscriptionResponse> mine() {
        return subscriptionService.mine(CurrentUser.get());
    }

    @GetMapping("/api/subscriptions/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionPreview preview(@RequestParam Long memberId, @RequestParam Long planId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return subscriptionService.preview(memberId, planId, startDate);
    }

    @PostMapping("/api/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponse subscribe(@Valid @RequestBody SubscriptionRequest req) {
        return subscriptionService.create(CurrentUser.get(), req);
    }

    @PutMapping("/api/subscriptions/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponse cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest req) {
        return subscriptionService.cancel(id, req == null ? null : req.reason());
    }
}
