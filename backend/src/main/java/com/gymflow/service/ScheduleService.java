package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.TrainerRepository;
import com.gymflow.repository.WorkoutLogRepository;
import com.gymflow.repository.WorkoutPlanRepository;
import com.gymflow.repository.WorkoutScheduleRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final int MIN_MINUTES = 15;
    private static final int MAX_MINUTES = 240;
    /** Hội viên được ghi nhận hoàn thành muộn tối đa 7 ngày (kể cả khi buổi đã bị tự đánh "Vắng"). */
    private static final int LATE_COMPLETE_DAYS = 7;

    private final WorkoutScheduleRepository scheduleRepository;
    private final WorkoutLogRepository logRepository;
    private final WorkoutPlanRepository planRepository;
    private final TrainerRepository trainerRepository;
    private final SubscriptionService subscriptionService;
    private final AccessService access;
    private final AuditService auditService;
    private final Mappers mappers;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(AuthUser user, LocalDateTime from, LocalDateTime to, Long memberId, Long trainerId) {
        if (from == null || to == null || !to.isAfter(from)) throw ApiException.badRequest("Khoảng thời gian không hợp lệ");
        if (Duration.between(from, to).toDays() > 93) throw ApiException.badRequest("Chỉ xem tối đa 3 tháng mỗi lần");
        Long scopeMember = memberId;
        Long scopeTrainer = trainerId;
        if (user.isMember()) {
            scopeMember = access.requireMemberOf(user).getId();
            scopeTrainer = null;
        } else if (user.isTrainer()) {
            Long myId = access.requireTrainerOf(user).getId();
            if (memberId != null) {
                access.requireView(user, memberId);
            } else {
                scopeTrainer = myId;
            }
        }
        return mappers.schedules(scheduleRepository.findInRange(from, to, scopeMember, scopeTrainer));
    }

    /**
     * Tạo buổi tập, có thể lặp hằng tuần. Buổi đầu tiên phải hợp lệ; các tuần sau nếu trùng giờ
     * hoặc hết gói thì bỏ qua và báo lại trong {@code skipped} thay vì huỷ cả loạt.
     */
    @Transactional
    public ScheduleCreatedResponse create(AuthUser user, ScheduleRequest req) {
        Member member = access.requireCoach(user, req.memberId());
        if (member.getStatus() != MemberStatus.ACTIVE) throw ApiException.badRequest("Hội viên không ở trạng thái hoạt động");
        Trainer trainer = resolveTrainer(user, member, req.trainerId());
        WorkoutPlan plan = resolvePlan(member, req.planId());
        validateTime(req.startTime(), req.endTime(), true);

        lockForConflictCheck(member, trainer);
        int repeat = req.repeatWeeks() == null ? 0 : req.repeatWeeks();
        List<WorkoutSchedule> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int w = 0; w <= repeat; w++) {
            LocalDateTime start = req.startTime().plusWeeks(w);
            LocalDateTime end = req.endTime().plusWeeks(w);
            String problem = conflict(member, trainer, start, end, null);
            if (problem != null) {
                if (w == 0) throw ApiException.conflict(problem);
                skipped.add(start.format(FMT) + ": " + problem);
                continue;
            }
            WorkoutSchedule s = new WorkoutSchedule();
            s.setMember(member);
            s.setTrainer(trainer);
            s.setPlan(plan);
            s.setTitle(req.title().trim());
            s.setStartTime(start);
            s.setEndTime(end);
            s.setNote(Mappers.blankToNull(req.note()));
            s.setStatus(ScheduleStatus.SCHEDULED);
            created.add(scheduleRepository.saveAndFlush(s));
        }
        auditService.log("SCHEDULE_CREATE", "SCHEDULE", created.get(0).getId(),
                member.getCode() + " · " + created.size() + " buổi từ " + req.startTime().format(FMT)
                        + (skipped.isEmpty() ? "" : " · bỏ qua " + skipped.size()));
        return new ScheduleCreatedResponse(mappers.schedules(created), skipped);
    }

    @Transactional
    public ScheduleResponse update(AuthUser user, Long id, ScheduleUpdateRequest req) {
        WorkoutSchedule s = load(id);
        if (!access.canCoach(user, s.getMember())) throw ApiException.forbidden();
        if (s.getStatus() != ScheduleStatus.SCHEDULED) throw ApiException.badRequest("Chỉ dời được buổi đang ở trạng thái \"Đã lên lịch\"");
        Trainer trainer = resolveTrainer(user, s.getMember(), req.trainerId());
        validateTime(req.startTime(), req.endTime(), !req.startTime().equals(s.getStartTime()));
        lockForConflictCheck(s.getMember(), trainer);
        String problem = conflict(s.getMember(), trainer, req.startTime(), req.endTime(), s.getId());
        if (problem != null) throw ApiException.conflict(problem);
        String before = s.getStartTime().format(FMT);
        s.setTrainer(trainer);
        s.setPlan(resolvePlan(s.getMember(), req.planId()));
        s.setTitle(req.title().trim());
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setNote(Mappers.blankToNull(req.note()));
        scheduleRepository.saveAndFlush(s);
        auditService.log("SCHEDULE_UPDATE", "SCHEDULE", s.getId(), before + " → " + s.getStartTime().format(FMT));
        return mappers.schedule(s);
    }

    @Transactional
    public ScheduleResponse cancel(AuthUser user, Long id) {
        WorkoutSchedule s = load(id);
        if (!access.canCoach(user, s.getMember())) throw ApiException.forbidden();
        if (s.getStatus() != ScheduleStatus.SCHEDULED) throw ApiException.badRequest("Chỉ huỷ được buổi chưa diễn ra");
        s.setStatus(ScheduleStatus.CANCELLED);
        auditService.log("SCHEDULE_CANCEL", "SCHEDULE", s.getId(), s.getMember().getCode() + " · " + s.getStartTime().format(FMT));
        return mappers.schedule(s);
    }

    @Transactional
    public ScheduleResponse markMissed(AuthUser user, Long id) {
        WorkoutSchedule s = load(id);
        if (!access.canCoach(user, s.getMember())) throw ApiException.forbidden();
        if (s.getStatus() != ScheduleStatus.SCHEDULED) throw ApiException.badRequest("Buổi này không còn ở trạng thái \"Đã lên lịch\"");
        if (s.getStartTime().isAfter(LocalDateTime.now(clock))) throw ApiException.badRequest("Buổi tập chưa bắt đầu");
        s.setStatus(ScheduleStatus.MISSED);
        auditService.log("SCHEDULE_MISSED", "SCHEDULE", s.getId(), s.getMember().getCode());
        return mappers.schedule(s);
    }

    /** Hội viên của buổi, HLV phụ trách hoặc quản lý ghi nhận hoàn thành. */
    @Transactional
    public ScheduleResponse complete(AuthUser user, Long id, CompleteRequest req) {
        WorkoutSchedule s = load(id);
        if (!access.canView(user, s.getMember())) throw ApiException.forbidden();
        LocalDateTime now = LocalDateTime.now(clock);
        if (s.getStatus() == ScheduleStatus.CANCELLED) throw ApiException.badRequest("Buổi tập đã bị huỷ");
        if (s.getStatus() == ScheduleStatus.COMPLETED) throw ApiException.conflict("Buổi tập này đã được ghi nhận hoàn thành");
        if (s.getStartTime().isAfter(now)) throw ApiException.badRequest("Chưa đến giờ tập, không thể đánh dấu hoàn thành");
        if (s.getEndTime().plusDays(LATE_COMPLETE_DAYS).isBefore(now)) {
            throw ApiException.badRequest("Đã quá " + LATE_COMPLETE_DAYS + " ngày, không thể ghi nhận buổi này");
        }
        WorkoutLog log = new WorkoutLog();
        log.setSchedule(s);
        log.setMember(s.getMember());
        log.setCompletedAt(now);
        log.setDurationMinutes(req.durationMinutes() != null ? req.durationMinutes()
                : (int) Duration.between(s.getStartTime(), s.getEndTime()).toMinutes());
        log.setCalories(req.calories());
        log.setRating(req.rating());
        log.setNote(Mappers.blankToNull(req.note()));
        logRepository.save(log);
        s.setStatus(ScheduleStatus.COMPLETED);
        auditService.log("SCHEDULE_COMPLETE", "SCHEDULE", s.getId(), s.getMember().getCode());
        return mappers.schedule(s, log);
    }

    /** 00:05 hằng ngày và khi khởi động: buổi đã qua giờ kết thúc mà chưa hoàn thành → "Vắng". */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void autoMarkMissed() {
        LocalDateTime cutoff = LocalDateTime.now(clock).toLocalDate().atStartOfDay();
        int n = scheduleRepository.markMissed(cutoff, ScheduleStatus.SCHEDULED, ScheduleStatus.MISSED);
        if (n > 0) {
            log.info("Tự đánh vắng {} buổi tập trước {}", n, cutoff);
            auditService.logAs(null, "system", "SCHEDULE_AUTO_MISSED", "SCHEDULE", null, n + " buổi", true);
        }
    }

    // ---------- helpers ----------
    private void validateTime(LocalDateTime start, LocalDateTime end, boolean mustBeFuture) {
        if (!end.isAfter(start)) throw ApiException.badRequest("Giờ kết thúc phải sau giờ bắt đầu");
        if (!start.toLocalDate().equals(end.toLocalDate())) throw ApiException.badRequest("Buổi tập phải bắt đầu và kết thúc trong cùng một ngày");
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw ApiException.badRequest("Buổi tập dài từ " + MIN_MINUTES + " đến " + MAX_MINUTES + " phút");
        }
        if (mustBeFuture && start.isBefore(LocalDateTime.now(clock))) throw ApiException.badRequest("Không xếp lịch vào thời điểm đã qua");
    }

    /** Khoá hội viên rồi HLV (thứ tự cố định, tránh deadlock) để kiểm tra trùng giờ không bị yêu cầu đồng thời vượt qua. */
    private void lockForConflictCheck(Member member, Trainer trainer) {
        scheduleRepository.lockMember(member.getId());
        if (trainer != null) scheduleRepository.lockTrainer(trainer.getId());
    }

    /** Trả về lý do không xếp được, hoặc null nếu hợp lệ. */
    String conflict(Member member, Trainer trainer, LocalDateTime start, LocalDateTime end, Long excludeId) {
        if (!subscriptionService.hasEffective(member.getId(), start.toLocalDate())) {
            return "hội viên không có gói tập hiệu lực vào ngày " + start.toLocalDate();
        }
        if (scheduleRepository.memberBusy(member.getId(), start, end, excludeId, ScheduleStatus.CANCELLED)) {
            return "hội viên đã có buổi tập khác trùng giờ";
        }
        if (trainer != null && scheduleRepository.trainerBusy(trainer.getId(), start, end, excludeId, ScheduleStatus.CANCELLED)) {
            return "HLV " + trainer.getUser().getFullName() + " đã có buổi khác trùng giờ";
        }
        return null;
    }

    private Trainer resolveTrainer(AuthUser user, Member member, Long trainerId) {
        if (user.isTrainer()) return access.requireTrainerOf(user);
        if (trainerId == null) return member.getTrainer();
        Trainer t = trainerRepository.findById(trainerId).orElseThrow(() -> ApiException.notFound("huấn luyện viên"));
        if (!t.isActive()) throw ApiException.badRequest("Huấn luyện viên đang ngừng hoạt động");
        return t;
    }

    private WorkoutPlan resolvePlan(Member member, Long planId) {
        if (planId == null) return null;
        WorkoutPlan p = planRepository.findById(planId).orElseThrow(() -> ApiException.notFound("giáo án"));
        if (!p.getMember().getId().equals(member.getId())) throw ApiException.badRequest("Giáo án không thuộc hội viên này");
        return p;
    }

    private WorkoutSchedule load(Long id) {
        return scheduleRepository.findById(id).orElseThrow(() -> ApiException.notFound("buổi tập"));
    }
}
