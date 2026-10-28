package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.MembershipPlan;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.MemberSubscriptionRepository;
import com.gymflow.repository.MembershipPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final MembershipPlanRepository planRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;
    private final Mappers mappers;

    @Transactional(readOnly = true)
    public List<PlanResponse> list(boolean includeInactive) {
        List<MembershipPlan> plans = includeInactive ? planRepository.findAllByOrderByPriceAsc()
                : planRepository.findByActiveTrueOrderByPriceAsc();
        return plans.stream().map(mappers::plan).toList();
    }

    @Transactional
    public PlanResponse create(PlanRequest req) {
        if (planRepository.existsByNameIgnoreCase(req.name().trim())) throw ApiException.conflict("Tên gói tập đã tồn tại");
        MembershipPlan p = new MembershipPlan();
        apply(p, req);
        p.setActive(true);
        planRepository.save(p);
        auditService.log("PLAN_CREATE", "PLAN", p.getId(), p.getName());
        return mappers.plan(p);
    }

    /** Sửa giá không ảnh hưởng gói đã bán: số tiền thực trả lưu riêng ở member_subscriptions.price_paid. */
    @Transactional
    public PlanResponse update(Long id, PlanRequest req) {
        MembershipPlan p = load(id);
        if (planRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) throw ApiException.conflict("Tên gói tập đã tồn tại");
        apply(p, req);
        auditService.log("PLAN_UPDATE", "PLAN", p.getId(), p.getName());
        return mappers.plan(p);
    }

    @Transactional
    public PlanResponse setActive(Long id, boolean active) {
        MembershipPlan p = load(id);
        p.setActive(active);
        auditService.log(active ? "PLAN_ACTIVATE" : "PLAN_DEACTIVATE", "PLAN", p.getId(), p.getName());
        return mappers.plan(p);
    }

    /** Gói đã từng được bán chỉ ngừng cung cấp, không xoá cứng (giữ lịch sử doanh thu). */
    @Transactional
    public void delete(Long id) {
        MembershipPlan p = load(id);
        if (subscriptionRepository.existsByPlanId(id)) {
            throw ApiException.conflict("Gói đã có người đăng ký, hãy chuyển sang \"Ngừng cung cấp\" thay vì xoá");
        }
        planRepository.delete(p);
        auditService.log("PLAN_DELETE", "PLAN", id, p.getName());
    }

    private void apply(MembershipPlan p, PlanRequest req) {
        p.setName(req.name().trim());
        p.setDescription(Mappers.blankToNull(req.description()));
        p.setPrice(req.price());
        p.setDurationDays(req.durationDays());
        p.setBenefits(Mappers.blankToNull(req.benefits()));
    }

    MembershipPlan load(Long id) {
        return planRepository.findById(id).orElseThrow(() -> ApiException.notFound("gói tập"));
    }
}
