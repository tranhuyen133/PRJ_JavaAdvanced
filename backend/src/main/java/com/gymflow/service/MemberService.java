package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.common.PageResponse;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.*;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TrainerRepository trainerRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final WeightRecordRepository weightRepository;
    private final UserService userService;
    private final AccessService access;
    private final AuditService auditService;
    private final Mappers mappers;

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> search(AuthUser user, String q, MemberStatus status, Long trainerId, int page, int size) {
        Long scopeTrainer = trainerId;
        if (user.isTrainer()) {
            // HLV chỉ tra cứu được hội viên của mình, bỏ qua trainerId gửi lên.
            scopeTrainer = access.requireTrainerOf(user).getId();
        } else if (!user.isAdmin()) {
            throw ApiException.forbidden();
        }
        return PageResponse.of(memberRepository.search(q == null ? "" : q.trim(), status, scopeTrainer,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200))), mappers::member);
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse detail(AuthUser user, Long id) {
        Member m = access.requireView(user, id);
        List<SubscriptionResponse> subs = subscriptionRepository.findByMemberIdOrderByStartDateDesc(id).stream()
                .map(mappers::subscription).toList();
        List<WorkoutPlanSummary> plans = workoutPlanRepository.findByMemberIdOrderByStartDateDesc(id).stream()
                .map(mappers::planSummary).toList();
        WeightResponse latest = mappers.weight(weightRepository.findFirstByMemberIdOrderByRecordedDateDesc(id).orElse(null));
        long total = scheduleRepository.countByMemberIdAndStatusNot(id, ScheduleStatus.CANCELLED);
        long done = scheduleRepository.countByMemberIdAndStatus(id, ScheduleStatus.COMPLETED);
        return new MemberDetailResponse(mappers.member(m), subs, plans, latest, total, done);
    }

    @Transactional(readOnly = true)
    public MemberResponse me(AuthUser user) {
        return mappers.member(access.requireMemberOf(user));
    }

    @Transactional
    public MemberCreatedResponse create(MemberCreateRequest req) {
        UserService.Created c = userService.createAccount(req.email(), req.fullName(), req.phone(), req.password(), Role.MEMBER);
        Member m = new Member();
        m.setUser(c.user());
        m.setCode(nextCode());
        m.setDateOfBirth(req.dateOfBirth());
        m.setGender(req.gender());
        m.setHeightCm(req.heightCm());
        m.setGoal(Mappers.blankToNull(req.goal()));
        m.setAddress(Mappers.blankToNull(req.address()));
        m.setNote(Mappers.blankToNull(req.note()));
        m.setJoinedAt(mappers.today());
        m.setStatus(MemberStatus.ACTIVE);
        m.setTrainer(resolveTrainer(req.trainerId()));
        memberRepository.save(m);
        auditService.log("MEMBER_CREATE", "MEMBER", m.getId(), m.getCode() + " – " + c.user().getFullName());
        return new MemberCreatedResponse(mappers.member(m), c.temporaryPassword());
    }

    /**
     * Hồ sơ hội viên cho tài khoản tự đăng ký: chưa có HLV, chưa có gói (gói mua tại quầy),
     * thông tin thể trạng hội viên tự bổ sung sau trong trang Hồ sơ.
     */
    @Transactional
    public Member createSelfRegistered(User user) {
        Member m = new Member();
        m.setUser(user);
        m.setCode(nextCode());
        m.setJoinedAt(mappers.today());
        m.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(m);
        auditService.logAs(user.getId(), user.getEmail(), "MEMBER_SELF_REGISTER", "MEMBER", m.getId(),
                m.getCode() + " – " + user.getFullName(), true);
        return m;
    }

    @Transactional
    public MemberResponse update(Long id, MemberUpdateRequest req) {
        Member m = access.loadMember(id);
        m.getUser().setFullName(req.fullName().trim());
        m.getUser().setPhone(Mappers.blankToNull(req.phone()));
        m.setDateOfBirth(req.dateOfBirth());
        m.setGender(req.gender());
        m.setHeightCm(req.heightCm());
        m.setGoal(Mappers.blankToNull(req.goal()));
        m.setAddress(Mappers.blankToNull(req.address()));
        m.setNote(Mappers.blankToNull(req.note()));
        m.setTrainer(resolveTrainer(req.trainerId()));
        auditService.log("MEMBER_UPDATE", "MEMBER", m.getId(), m.getCode());
        return mappers.member(m);
    }

    @Transactional
    public MemberResponse updateSelf(AuthUser user, MemberSelfUpdateRequest req) {
        Member m = access.requireMemberOf(user);
        m.getUser().setPhone(Mappers.blankToNull(req.phone()));
        m.setDateOfBirth(req.dateOfBirth());
        m.setGender(req.gender());
        m.setHeightCm(req.heightCm());
        m.setGoal(Mappers.blankToNull(req.goal()));
        m.setAddress(Mappers.blankToNull(req.address()));
        auditService.log("MEMBER_SELF_UPDATE", "MEMBER", m.getId(), null);
        return mappers.member(m);
    }

    @Transactional
    public MemberResponse setStatus(Long id, MemberStatus status) {
        Member m = access.loadMember(id);
        m.setStatus(status);
        auditService.log("MEMBER_STATUS", "MEMBER", m.getId(), m.getCode() + " → " + status);
        return mappers.member(m);
    }

    @Transactional
    public MemberResponse assignTrainer(Long id, Long trainerId) {
        Member m = access.loadMember(id);
        m.setTrainer(resolveTrainer(trainerId));
        auditService.log("MEMBER_ASSIGN_TRAINER", "MEMBER", m.getId(),
                m.getCode() + " → " + (m.getTrainer() == null ? "không có HLV" : m.getTrainer().getUser().getFullName()));
        return mappers.member(m);
    }

    private Trainer resolveTrainer(Long trainerId) {
        if (trainerId == null) return null;
        Trainer t = trainerRepository.findById(trainerId).orElseThrow(() -> ApiException.notFound("huấn luyện viên"));
        if (!t.isActive()) throw ApiException.badRequest("Huấn luyện viên này đang ngừng nhận hội viên");
        return t;
    }

    /** Mã HV00001, HV00002… tăng theo id lớn nhất; ràng buộc uq_members_code chặn trùng nếu chạy song song. */
    private String nextCode() {
        long next = memberRepository.maxId() + 1;
        String code = String.format("HV%05d", next);
        while (memberRepository.existsByCode(code)) {
            code = String.format("HV%05d", ++next);
        }
        return code;
    }
}
