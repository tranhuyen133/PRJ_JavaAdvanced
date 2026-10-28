package com.gymflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "member_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class MemberSubscription {
    public static final int EXPIRING_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "price_paid", nullable = false)
    private Long pricePaid;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Trạng thái tính theo ngày, không lưu cứng trong CSDL. */
    public SubscriptionState stateOn(LocalDate today) {
        if (cancelled) return SubscriptionState.CANCELLED;
        if (today.isBefore(startDate)) return SubscriptionState.UPCOMING;
        if (today.isAfter(endDate)) return SubscriptionState.EXPIRED;
        if (ChronoUnit.DAYS.between(today, endDate) <= EXPIRING_DAYS) return SubscriptionState.EXPIRING;
        return SubscriptionState.ACTIVE;
    }

    public boolean isEffectiveOn(LocalDate day) {
        return !cancelled && !day.isBefore(startDate) && !day.isAfter(endDate);
    }

    public long daysLeft(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(today, endDate));
    }
}
