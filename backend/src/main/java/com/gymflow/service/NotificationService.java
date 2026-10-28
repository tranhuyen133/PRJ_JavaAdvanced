package com.gymflow.service;

import com.gymflow.common.PageResponse;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.*;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Thông báo tính sẵn từ dữ liệu hiện có (không cần bảng riêng):
 * gói sắp hết hạn / đã hết, buổi tập hôm nay, buổi cần ghi nhận, nhắc cân định kỳ.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final MemberSubscriptionRepository subscriptionRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final WeightRecordRepository weightRepository;
    private final MemberRepository memberRepository;
    private final AuditLogRepository auditRepository;
    private final AccessService access;
    private final Mappers mappers;
    private final Clock clock;

    public List<NotificationResponse> forUser(AuthUser user) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        List<NotificationResponse> out = new ArrayList<>();
        if (user.isMember()) {
            Member m = access.requireMemberOf(user);
            MemberSubscription cur = mappers.pickCurrent(subscriptionRepository.findByMemberIdOrderByStartDateDesc(m.getId()), today);
            if (cur == null || cur.stateOn(today) == SubscriptionState.EXPIRED) {
                out.add(n("sub-none", "danger", "Bạn chưa có gói tập hiệu lực", "Liên hệ quầy lễ tân để đăng ký hoặc gia hạn.", "/me/packages", now));
            } else if (cur.stateOn(today) == SubscriptionState.EXPIRING) {
                out.add(n("sub-exp-" + cur.getId(), "warning", "Gói " + cur.getPlan().getName() + " sắp hết hạn",
                        "Còn " + cur.daysLeft(today) + " ngày (hết hạn " + cur.getEndDate() + ").", "/me/packages", now));
            }
            for (WorkoutSchedule s : scheduleRepository.findByMemberIdAndStartTimeBetweenOrderByStartTimeAsc(
                    m.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
                if (s.getStatus() == ScheduleStatus.SCHEDULED) {
                    boolean started = !s.getStartTime().isAfter(now);
                    out.add(n("ws-" + s.getId(), started ? "warning" : "info",
                            started ? "Đánh dấu hoàn thành: " + s.getTitle() : "Hôm nay " + s.getStartTime().format(HM) + ": " + s.getTitle(),
                            s.getTrainer() == null ? "Tự tập" : "Với HLV " + s.getTrainer().getUser().getFullName(), "/me/schedule", s.getStartTime()));
                }
            }
            WeightRecord w = weightRepository.findFirstByMemberIdOrderByRecordedDateDesc(m.getId()).orElse(null);
            if (w == null || w.getRecordedDate().isBefore(today.minusDays(7))) {
                out.add(n("weight", "info", "Cập nhật cân nặng", "Đã hơn 7 ngày bạn chưa ghi cân nặng.", "/me/progress", now));
            }
        } else if (user.isTrainer()) {
            Trainer t = access.requireTrainerOf(user);
            List<WorkoutSchedule> todayList = scheduleRepository.findInRange(today.atStartOfDay(), today.plusDays(1).atStartOfDay(), null, t.getId());
            long pending = todayList.stream().filter(s -> s.getStatus() == ScheduleStatus.SCHEDULED).count();
            if (pending > 0) out.add(n("today", "info", "Hôm nay có " + pending + " buổi tập", "Xem lịch để chuẩn bị giáo án.", "/schedules", now));
            List<Long> mine = memberRepository.findByTrainerId(t.getId()).stream().map(Member::getId).toList();
            subscriptionRepository.findEndingBetween(today, today.plusDays(MemberSubscription.EXPIRING_DAYS)).stream()
                    .filter(s -> mine.contains(s.getMember().getId()))
                    .forEach(s -> out.add(n("sub-" + s.getId(), "warning", s.getMember().getUser().getFullName() + " sắp hết gói",
                            "Hết hạn " + s.getEndDate() + ", nhắc hội viên gia hạn.", "/members/" + s.getMember().getId(), now)));
        } else {
            long expiring = subscriptionRepository.findEndingBetween(today, today.plusDays(MemberSubscription.EXPIRING_DAYS)).size();
            if (expiring > 0) out.add(n("expiring", "warning", expiring + " gói sắp hết hạn trong 7 ngày", "Liên hệ hội viên để gia hạn.", "/subscriptions?state=EXPIRING", now));
            long failed = auditRepository.search("", "LOGIN_FAILED", now.minusDays(1), now.plusMinutes(1), PageRequest.of(0, 1)).getTotalElements();
            if (failed >= 5) out.add(n("login-failed", "danger", failed + " lần đăng nhập thất bại trong 24 giờ", "Kiểm tra nhật ký hệ thống.", "/audit-logs?action=LOGIN_FAILED", now));
        }
        return out;
    }

    public PageResponse<AuditResponse> audit(String q, String action, LocalDate from, LocalDate to, int page, int size) {
        LocalDateTime f = (from == null ? LocalDate.of(2000, 1, 1) : from).atStartOfDay();
        LocalDateTime t = (to == null ? LocalDate.of(2100, 1, 1) : to.plusDays(1)).atStartOfDay();
        return PageResponse.of(auditRepository.search(q == null ? "" : q.trim(), action == null ? "" : action.trim(), f, t,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200))), mappers::audit);
    }

    public List<String> auditActions() {
        return auditRepository.distinctActions();
    }

    private static NotificationResponse n(String id, String level, String title, String msg, String link, LocalDateTime at) {
        return new NotificationResponse(id, level, title, msg, link, at);
    }
}
