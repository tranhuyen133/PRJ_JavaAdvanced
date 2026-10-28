package com.gymflow.config;

import com.gymflow.domain.Role;
import com.gymflow.dto.Dtos.DemoAccount;

import java.util.List;

/**
 * Tài khoản demo do DataSeeder tạo, dùng chung cho nút "Đăng nhập nhanh".
 * Chỉ dành cho môi trường phát triển / trình diễn: khi triển khai thật đặt SEED_DEMO_DATA=false và DEMO_QUICK_LOGIN=false.
 */
public final class DemoAccounts {
    private DemoAccounts() {
    }

    public static final DemoAccount ADMIN = new DemoAccount("Quản lý", Role.ADMIN, "admin@gymflow.vn", "Admin@123");
    public static final DemoAccount TRAINER = new DemoAccount("Huấn luyện viên", Role.TRAINER, "trainer@gymflow.vn", "Trainer@123");
    public static final DemoAccount MEMBER = new DemoAccount("Hội viên", Role.MEMBER, "member@gymflow.vn", "Member@123");

    public static final List<DemoAccount> ALL = List.of(ADMIN, TRAINER, MEMBER);
}
