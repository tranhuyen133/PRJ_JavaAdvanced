package com.gymflow.web;

import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.ProgressService;
import com.gymflow.service.ScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Lịch tập & tiến độ")
@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ProgressService progressService;

    @GetMapping("/api/schedules")
    public List<ScheduleResponse> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                       @RequestParam(required = false) Long memberId,
                                       @RequestParam(required = false) Long trainerId) {
        return scheduleService.list(CurrentUser.get(), from, to, memberId, trainerId);
    }

    @PostMapping("/api/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleCreatedResponse create(@Valid @RequestBody ScheduleRequest req) {
        return scheduleService.create(CurrentUser.get(), req);
    }

    @PutMapping("/api/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleUpdateRequest req) {
        return scheduleService.update(CurrentUser.get(), id, req);
    }

    @PutMapping("/api/schedules/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleResponse cancel(@PathVariable Long id) {
        return scheduleService.cancel(CurrentUser.get(), id);
    }

    @PutMapping("/api/schedules/{id}/missed")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ScheduleResponse missed(@PathVariable Long id) {
        return scheduleService.markMissed(CurrentUser.get(), id);
    }

    @PostMapping("/api/schedules/{id}/complete")
    public ScheduleResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest req) {
        return scheduleService.complete(CurrentUser.get(), id, req);
    }

    @DeleteMapping("/api/weights/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWeight(@PathVariable Long id) {
        progressService.deleteWeight(CurrentUser.get(), id);
    }
}
