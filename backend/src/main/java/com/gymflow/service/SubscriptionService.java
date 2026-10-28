package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.MemberSubscriptionRepository;
import com.gymflow.repository.UserRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanService planService;
    private final AccessService access;
    private final AuditService auditService;
    private final Mappers mappers;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list(SubscriptionState state, Long memberId) {
        List<MemberSubscription> all = memberId == null ? subscriptionRepository.findAllByOrderByCreatedAtDesc()
                : subscriptionRepository.findByMemberIdOrderByStartDateDesc(memberId);
        LocalDate today = mappers.today();
        return all.stream().filter(s -> state == null || s.stateOn(today) == state).map(mappers::subscription).toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> ofMember(AuthUser user, Long memberId) {
        access.requireView(user, memberId);
        return subscriptionRepository.findByMemberIdOrderByStartDateDesc(memberId).stream().map(mappers::subscription).toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> mine(AuthUser user) {
        return ofMember(user, access.requireMemberOf(user).getId());
    }

    /** Gợi ý ngày cho form đăng ký: để trống ngày bắt đầu thì nối tiếp gói đang có. */
    @Transactional(readOnly = true)
    public SubscriptionPreview preview(Long memberId, Long planId, LocalDate startDate) {
        access.loadMember(memberId);
        MembershipPlan plan = planService.load(planId);
        LocalDate start = startDate;
        boolean chained = false;
        if (start == null) {
            LocalDate today = mappers.today();
            LocalDate maxEnd = subscriptionRepository.maxEndDate(memberId);
            if (maxEnd != null && !maxEnd.isBefore(today)) {
                start = maxEnd.plusDays(1);
                chained = true;
            } else {
                start = today;
            }
        }
        return new SubscriptionPreview(start, start.plusDays(plan.getDurationDays() - 1L), plan.getPrice(), chained);
    }

    @Transactional
    public SubscriptionResponse create(AuthUser actor, SubscriptionRequest req) {
        Member member = access.loadMember(req.memberId());
        if (member.getStatus() == MemberStatus.INACTIVE) {
            throw ApiException.badRequest("Hội viên đã ngừng hoạt động, hãy kích hoạt lại trước khi đăng ký gói");
        }
        MembershipPlan plan = planService.load(req.planId());
        if (!plan.isActive()) throw ApiException.badRequest("Gói tập này đã ngừng cung cấp");
        // Khoá dòng hội viên trước khi tính ngày nối tiếp và kiểm tra trùng: hai yêu cầu đồng thời cho cùng
        // hội viên phải chờ nhau, nên yêu cầu sau luôn thấy gói vừa tạo (cần READ COMMITTED – xem application.yml).
        subscriptionRepository.lockMember(member.getId());
        SubscriptionPreview p = preview(member.getId(), plan.getId(), req.startDate());
        if (req.startDate() != null && req.startDate().isBefore(mappers.today().minusDays(30))) {
            throw ApiException.badRequest("Ngày bắt đầu không được lùi quá 30 ngày");
        }
        if (subscriptionRepository.existsOverlap(member.getId(), p.startDate(), p.endDate())) {
            throw ApiException.conflict("Hội viên đã có gói tập trùng khoảng " + p.startDate() + " – " + p.endDate()
                    + ". Để trống ngày bắt đầu để gia hạn nối tiếp.");
        }
        MemberSubscription s = new MemberSubscription();
        s.setMember(member);
        s.setPlan(plan);
        s.setStartDate(p.startDate());
        s.setEndDate(p.endDate());
        s.setPricePaid(req.pricePaid() == null ? plan.getPrice() : req.pricePaid());
        s.setNote(Mappers.blankToNull(req.note()));
        s.setCreatedBy(userRepository.getReferenceById(actor.id()));
        subscriptionRepository.saveAndFlush(s);
        auditService.log(p.chained() ? "SUBSCRIPTION_RENEW" : "SUBSCRIPTION_CREATE", "SUBSCRIPTION", s.getId(),
                member.getCode() + " · " + plan.getName() + " · " + s.getStartDate() + " → " + s.getEndDate());
        return mappers.subscription(s);
    }

    @Transactional
    public SubscriptionResponse cancel(Long id, String reason) {
        MemberSubscription s = subscriptionRepository.findById(id).orElseThrow(() -> ApiException.notFound("đăng ký gói"));
        if (s.isCancelled()) throw ApiException.badRequest("Gói này đã bị huỷ trước đó");
        if (s.stateOn(mappers.today()) == SubscriptionState.EXPIRED) throw ApiException.badRequest("Không thể huỷ gói đã hết hạn");
        s.setCancelled(true);
        s.setCancelledAt(LocalDateTime.now(clock));
        String r = Mappers.blankToNull(reason);
        if (r != null) {
            String merged = s.getNote() == null ? "Huỷ: " + r : s.getNote() + " | Huỷ: " + r;
            s.setNote(merged.length() > 255 ? merged.substring(0, 255) : merged);
        }
        auditService.log("SUBSCRIPTION_CANCEL", "SUBSCRIPTION", s.getId(), s.getMember().getCode() + (r == null ? "" : " · " + r));
        return mappers.subscription(s);
    }

    /** Hội viên có gói hiệu lực vào ngày này không (dùng khi xếp lịch). */
    @Transactional(readOnly = true)
    public boolean hasEffective(Long memberId, LocalDate day) {
        return !subscriptionRepository.findEffective(memberId, day).isEmpty();
    }
}
