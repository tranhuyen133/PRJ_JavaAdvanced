package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.Role;
import com.gymflow.domain.Trainer;
import com.gymflow.domain.TrainerShift;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.TrainerRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final UserService userService;
    private final AccessService access;
    private final AuditService auditService;
    private final Mappers mappers;

    @Transactional(readOnly = true)
    public List<TrainerResponse> list() {
        return trainerRepository.findAllByOrderByIdAsc().stream().map(mappers::trainer).toList();
    }

    @Transactional(readOnly = true)
    public TrainerResponse get(Long id) {
        return mappers.trainer(load(id));
    }

    @Transactional(readOnly = true)
    public TrainerResponse me(AuthUser user) {
        return mappers.trainer(access.requireTrainerOf(user));
    }

    @Transactional
    public TrainerCreatedResponse create(TrainerCreateRequest req) {
        UserService.Created c = userService.createAccount(req.email(), req.fullName(), req.phone(), req.password(), Role.TRAINER);
        Trainer t = new Trainer();
        t.setUser(c.user());
        t.setSpecialty(Mappers.blankToNull(req.specialty()));
        t.setExperienceYears(req.experienceYears() == null ? 0 : req.experienceYears());
        t.setCertificates(Mappers.blankToNull(req.certificates()));
        t.setBio(Mappers.blankToNull(req.bio()));
        t.setActive(true);
        trainerRepository.save(t);
        auditService.log("TRAINER_CREATE", "TRAINER", t.getId(), c.user().getFullName());
        return new TrainerCreatedResponse(mappers.trainer(t), c.temporaryPassword());
    }

    @Transactional
    public TrainerResponse update(AuthUser user, Long id, TrainerUpdateRequest req) {
        Trainer t = load(id);
        access.requireTrainerSelfOrAdmin(user, t);
        t.getUser().setFullName(req.fullName().trim());
        t.getUser().setPhone(Mappers.blankToNull(req.phone()));
        t.setSpecialty(Mappers.blankToNull(req.specialty()));
        t.setExperienceYears(req.experienceYears() == null ? 0 : req.experienceYears());
        t.setCertificates(Mappers.blankToNull(req.certificates()));
        t.setBio(Mappers.blankToNull(req.bio()));
        auditService.log("TRAINER_UPDATE", "TRAINER", t.getId(), t.getUser().getFullName());
        return mappers.trainer(t);
    }

    @Transactional
    public TrainerResponse setActive(Long id, boolean active) {
        Trainer t = load(id);
        t.setActive(active);
        auditService.log(active ? "TRAINER_ACTIVATE" : "TRAINER_DEACTIVATE", "TRAINER", t.getId(), t.getUser().getFullName());
        return mappers.trainer(t);
    }

    /** Thay toàn bộ ca làm; chặn ca chồng nhau trong cùng một ngày. */
    @Transactional
    public TrainerResponse replaceShifts(AuthUser user, Long id, List<ShiftRequest> shifts) {
        Trainer t = load(id);
        access.requireTrainerSelfOrAdmin(user, t);
        List<ShiftRequest> sorted = new ArrayList<>(shifts);
        sorted.sort(Comparator.comparing(ShiftRequest::dayOfWeek).thenComparing(ShiftRequest::startTime));
        for (int i = 0; i < sorted.size(); i++) {
            ShiftRequest s = sorted.get(i);
            if (!s.endTime().isAfter(s.startTime())) {
                throw ApiException.badRequest("Ca " + dayName(s.dayOfWeek()) + ": giờ kết thúc phải sau giờ bắt đầu");
            }
            if (i > 0) {
                ShiftRequest prev = sorted.get(i - 1);
                if (prev.dayOfWeek().equals(s.dayOfWeek()) && s.startTime().isBefore(prev.endTime())) {
                    throw ApiException.badRequest("Các ca " + dayName(s.dayOfWeek()) + " bị chồng giờ nhau");
                }
            }
        }
        t.getShifts().clear();
        for (ShiftRequest s : sorted) {
            TrainerShift shift = new TrainerShift();
            shift.setTrainer(t);
            shift.setDayOfWeek(s.dayOfWeek());
            shift.setStartTime(s.startTime());
            shift.setEndTime(s.endTime());
            t.getShifts().add(shift);
        }
        trainerRepository.saveAndFlush(t);
        auditService.log("TRAINER_SHIFTS", "TRAINER", t.getId(), sorted.size() + " ca/tuần");
        return mappers.trainer(t);
    }

    private Trainer load(Long id) {
        return trainerRepository.findById(id).orElseThrow(() -> ApiException.notFound("huấn luyện viên"));
    }

    static String dayName(int d) {
        return d == 7 ? "Chủ nhật" : "Thứ " + (d + 1);
    }
}
