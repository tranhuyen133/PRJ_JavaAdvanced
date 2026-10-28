package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.Member;
import com.gymflow.domain.Trainer;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.TrainerRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Phạm vi dữ liệu theo vai trò, kiểm tra ở tầng service (không chỉ ẩn nút ở FE):
 * ADMIN thấy tất cả · TRAINER chỉ hội viên mình phụ trách · MEMBER chỉ dữ liệu của chính mình.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessService {

    private final MemberRepository memberRepository;
    private final TrainerRepository trainerRepository;

    public Member requireMemberOf(AuthUser user) {
        return memberRepository.findByUserId(user.id())
                .orElseThrow(() -> ApiException.notFound("hồ sơ hội viên của tài khoản này"));
    }

    public Trainer requireTrainerOf(AuthUser user) {
        return trainerRepository.findByUserId(user.id())
                .orElseThrow(() -> ApiException.notFound("hồ sơ huấn luyện viên của tài khoản này"));
    }

    public Long trainerIdOrNull(AuthUser user) {
        return user.isTrainer() ? trainerRepository.findByUserId(user.id()).map(Trainer::getId).orElse(null) : null;
    }

    public Long memberIdOrNull(AuthUser user) {
        return user.isMember() ? memberRepository.findByUserId(user.id()).map(Member::getId).orElse(null) : null;
    }

    public Member loadMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> ApiException.notFound("hội viên"));
    }

    /** Được xem dữ liệu của hội viên. */
    public boolean canView(AuthUser user, Member member) {
        if (user.isAdmin()) return true;
        if (user.isMember()) return Objects.equals(member.getUser().getId(), user.id());
        if (user.isTrainer()) {
            return member.getTrainer() != null && Objects.equals(member.getTrainer().getUser().getId(), user.id());
        }
        return false;
    }

    /** Được ghi giáo án, lịch, cân nặng cho hội viên: admin hoặc HLV phụ trách. */
    public boolean canCoach(AuthUser user, Member member) {
        if (user.isAdmin()) return true;
        return user.isTrainer() && member.getTrainer() != null
                && Objects.equals(member.getTrainer().getUser().getId(), user.id());
    }

    public Member requireView(AuthUser user, Long memberId) {
        Member m = loadMember(memberId);
        if (!canView(user, m)) throw ApiException.forbidden();
        return m;
    }

    public Member requireCoach(AuthUser user, Long memberId) {
        Member m = loadMember(memberId);
        if (!canCoach(user, m)) throw ApiException.forbidden();
        return m;
    }

    public void requireTrainerSelfOrAdmin(AuthUser user, Trainer trainer) {
        if (user.isAdmin()) return;
        if (user.isTrainer() && Objects.equals(trainer.getUser().getId(), user.id())) return;
        throw ApiException.forbidden();
    }
}
