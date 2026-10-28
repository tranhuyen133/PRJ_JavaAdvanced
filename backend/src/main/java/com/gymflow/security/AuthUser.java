package com.gymflow.security;

import com.gymflow.domain.Role;

/** Principal gắn vào SecurityContext sau khi xác thực JWT. */
public record AuthUser(Long id, String email, Role role, String fullName) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isTrainer() {
        return role == Role.TRAINER;
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }
}
