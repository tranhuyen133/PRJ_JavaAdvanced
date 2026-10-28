package com.gymflow.web;

import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.TrainerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Huấn luyện viên")
@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerController {

    private final TrainerService trainerService;

    @GetMapping
    public List<TrainerResponse> list() {
        return trainerService.list();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRAINER')")
    public TrainerResponse me() {
        return trainerService.me(CurrentUser.get());
    }

    @GetMapping("/{id}")
    public TrainerResponse get(@PathVariable Long id) {
        return trainerService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerCreatedResponse create(@Valid @RequestBody TrainerCreateRequest req) {
        return trainerService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public TrainerResponse update(@PathVariable Long id, @Valid @RequestBody TrainerUpdateRequest req) {
        return trainerService.update(CurrentUser.get(), id, req);
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerResponse active(@PathVariable Long id, @Valid @RequestBody ActiveRequest req) {
        return trainerService.setActive(id, req.active());
    }

    @PutMapping("/{id}/shifts")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public TrainerResponse shifts(@PathVariable Long id, @Valid @RequestBody ShiftsRequest req) {
        return trainerService.replaceShifts(CurrentUser.get(), id, req.shifts());
    }
}
