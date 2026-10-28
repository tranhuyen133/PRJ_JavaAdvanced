package com.gymflow.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MemberSubscriptionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    private static MemberSubscription sub(LocalDate start, LocalDate end) {
        MemberSubscription s = new MemberSubscription();
        s.setStartDate(start);
        s.setEndDate(end);
        return s;
    }

    @Test
    void stateIsDerivedFromToday() {
        assertThat(sub(TODAY.plusDays(1), TODAY.plusDays(30)).stateOn(TODAY)).isEqualTo(SubscriptionState.UPCOMING);
        assertThat(sub(TODAY.minusDays(10), TODAY.plusDays(20)).stateOn(TODAY)).isEqualTo(SubscriptionState.ACTIVE);
        assertThat(sub(TODAY.minusDays(10), TODAY.plusDays(7)).stateOn(TODAY)).isEqualTo(SubscriptionState.EXPIRING);
        assertThat(sub(TODAY.minusDays(10), TODAY).stateOn(TODAY)).isEqualTo(SubscriptionState.EXPIRING);
        assertThat(sub(TODAY.minusDays(30), TODAY.minusDays(1)).stateOn(TODAY)).isEqualTo(SubscriptionState.EXPIRED);
    }

    @Test
    void cancelledWinsOverDates() {
        MemberSubscription s = sub(TODAY.minusDays(1), TODAY.plusDays(30));
        s.setCancelled(true);
        assertThat(s.stateOn(TODAY)).isEqualTo(SubscriptionState.CANCELLED);
        assertThat(s.isEffectiveOn(TODAY)).isFalse();
    }

    @Test
    void effectiveIncludesBothEndpoints() {
        MemberSubscription s = sub(TODAY, TODAY.plusDays(29));
        assertThat(s.isEffectiveOn(TODAY)).isTrue();
        assertThat(s.isEffectiveOn(TODAY.plusDays(29))).isTrue();
        assertThat(s.isEffectiveOn(TODAY.plusDays(30))).isFalse();
        assertThat(s.daysLeft(TODAY)).isEqualTo(29);
        assertThat(s.daysLeft(TODAY.plusDays(40))).isZero();
    }
}
