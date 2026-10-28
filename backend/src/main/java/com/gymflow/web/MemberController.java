package com.gymflow.web;

import com.gymflow.common.PageResponse;
import com.gymflow.domain.MemberStatus;
import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.MemberService;
import com.gymflow.service.ProgressService;
import com.gymflow.service.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Hội viên")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;
    private final ProgressService progressService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public PageResponse<MemberResponse> search(@RequestParam(defaultValue = "") String q,
                                               @RequestParam(required = false) MemberStatus status,
                                               @RequestParam(required = false) Long trainerId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return memberService.search(CurrentUser.get(), q, status, trainerId, page, size);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse me() {
        return memberService.me(CurrentUser.get());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse updateMe(@Valid @RequestBody MemberSelfUpdateRequest req) {
        return memberService.updateSelf(CurrentUser.get(), req);
    }

    @GetMapping("/{id}")
    public MemberDetailResponse detail(@PathVariable Long id) {
        return memberService.detail(CurrentUser.get(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MemberCreatedResponse create(@Valid @RequestBody MemberCreateRequest req) {
        return memberService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest req) {
        return memberService.update(id, req);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse status(@PathVariable Long id, @Valid @RequestBody MemberStatusRequest req) {
        return memberService.setStatus(id, req.status());
    }

    @PutMapping("/{id}/trainer")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse assignTrainer(@PathVariable Long id, @RequestBody AssignTrainerRequest req) {
        return memberService.assignTrainer(id, req.trainerId());
    }

    @GetMapping("/{id}/subscriptions")
    public List<SubscriptionResponse> subscriptions(@PathVariable Long id) {
        return subscriptionService.ofMember(CurrentUser.get(), id);
    }

    @GetMapping("/{id}/weights")
    public List<WeightResponse> weights(@PathVariable Long id) {
        return progressService.weights(CurrentUser.get(), id);
    }

    @PostMapping("/{id}/weights")
    public WeightResponse addWeight(@PathVariable Long id, @Valid @RequestBody WeightRequest req) {
        return progressService.upsertWeight(CurrentUser.get(), id, req);
    }

    @GetMapping("/{id}/progress")
    public ProgressResponse progress(@PathVariable Long id) {
        return progressService.progress(CurrentUser.get(), id);
    }
}
