package com.gymflow.config;

import com.gymflow.domain.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sinh dữ liệu mẫu khi CSDL trống (SEED_DEMO_DATA=true). Dữ liệu bám theo ngày hiện tại nên
 * lúc nào chạy cũng có gói còn hạn, gói sắp hết, buổi tập hôm nay và doanh thu 6 tháng gần nhất.
 * Mọi bản ghi đều thoả ràng buộc của Flyway V1 (không chồng gói, không trùng lịch HLV/hội viên).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final WorkoutScheduleRepository scheduleRepository;
    private final WorkoutLogRepository logRepository;
    private final WeightRecordRepository weightRepository;
    private final AuditLogRepository auditRepository;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Value("${app.seed-demo-data:false}")
    private boolean enabled;

    private static final String[] MEMBER_NAMES = {
            "Nguyễn Minh Anh", "Trần Quốc Bảo", "Lê Thu Hà", "Phạm Đức Huy", "Hoàng Gia Linh", "Vũ Thành Nam",
            "Đỗ Khánh Vy", "Bùi Tuấn Kiệt", "Ngô Phương Thảo", "Đặng Hải Long", "Phan Ngọc Mai", "Trịnh Văn Sơn"};
    private static final String[] GOALS = {"Giảm mỡ, săn chắc", "Tăng cơ", "Cải thiện sức bền", "Giảm cân", "Duy trì vóc dáng"};
    private static final int[] HOURS = {6, 8, 17, 19};

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.count() > 0) return;
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        log.info("Đang sinh dữ liệu mẫu GymFlow cho ngày {}", today);

        User admin = user(DemoAccounts.ADMIN.email(), DemoAccounts.ADMIN.password(), "Quản lý GymFlow", "0901000001", Role.ADMIN);

        // ---------- HLV & ca làm ----------
        Trainer[] trainers = {
                trainer(user(DemoAccounts.TRAINER.email(), DemoAccounts.TRAINER.password(), "Lê Hoàng Phúc", "0902000001", Role.TRAINER),
                        "Thể hình, giảm mỡ", 6, "NASM-CPT, Sơ cứu cơ bản", "6 năm huấn luyện, chuyên giáo án giảm mỡ cho người đi làm."),
                trainer(user("trainer2@gymflow.vn", "Trainer@123", "Nguyễn Thảo Vy", "0902000002", Role.TRAINER),
                        "Yoga, phục hồi chức năng", 4, "RYT-200", "Chuyên các bài mobility và phục hồi chấn thương nhẹ."),
                trainer(user("trainer3@gymflow.vn", "Trainer@123", "Trần Đức Mạnh", "0902000003", Role.TRAINER),
                        "Sức mạnh, powerlifting", 8, "ISSA, VN Powerlifting L2", "Từng thi đấu powerlifting, tập trung kỹ thuật squat – deadlift.")};
        for (Trainer t : trainers) {
            for (int d = 1; d <= 6; d++) {
                shift(t, d, LocalTime.of(5, 30), LocalTime.of(11, 30));
                shift(t, d, LocalTime.of(16, 0), LocalTime.of(21, 0));
            }
            trainerRepository.save(t);
        }

        // ---------- Gói tập ----------
        MembershipPlan p1 = plan("Gói 1 tháng", 30, 500_000L, "Tập không giới hạn giờ mở cửa", "Tủ đồ, nước uống miễn phí", true);
        MembershipPlan p3 = plan("Gói 3 tháng", 90, 1_350_000L, "Tiết kiệm 10% so với gói tháng", "Tủ đồ, 1 buổi đo InBody", true);
        MembershipPlan p6 = plan("Gói 6 tháng", 180, 2_500_000L, "Tiết kiệm 17%", "Tủ đồ, 2 buổi đo InBody, 1 buổi PT", true);
        MembershipPlan p12 = plan("Gói 12 tháng", 365, 4_500_000L, "Tiết kiệm 25%, bảo lưu 30 ngày", "Toàn bộ quyền lợi + 4 buổi PT", true);
        MembershipPlan pt = plan("PT 1 kèm 1 – 12 buổi", 30, 3_600_000L, "12 buổi với huấn luyện viên riêng", "Giáo án cá nhân, theo dõi dinh dưỡng", true);
        plan("Gói sinh viên (cũ)", 30, 350_000L, "Đã ngừng cung cấp từ quý trước", null, false);

        // ---------- Bài tập ----------
        List<Exercise> ex = new ArrayList<>();
        Object[][] exData = {
                {"Bench Press", MuscleGroup.CHEST, "Thanh đòn, ghế phẳng", Level.INTERMEDIATE},
                {"Incline Dumbbell Press", MuscleGroup.CHEST, "Tạ đơn, ghế dốc", Level.INTERMEDIATE},
                {"Hít đất (Push-up)", MuscleGroup.CHEST, "Không", Level.BEGINNER},
                {"Cable Fly", MuscleGroup.CHEST, "Máy cáp", Level.BEGINNER},
                {"Deadlift", MuscleGroup.BACK, "Thanh đòn", Level.ADVANCED},
                {"Lat Pulldown", MuscleGroup.BACK, "Máy kéo xô", Level.BEGINNER},
                {"Seated Cable Row", MuscleGroup.BACK, "Máy cáp", Level.BEGINNER},
                {"Hít xà (Pull-up)", MuscleGroup.BACK, "Xà đơn", Level.INTERMEDIATE},
                {"Back Squat", MuscleGroup.LEGS, "Thanh đòn, khung squat", Level.INTERMEDIATE},
                {"Leg Press", MuscleGroup.LEGS, "Máy đạp đùi", Level.BEGINNER},
                {"Romanian Deadlift", MuscleGroup.LEGS, "Thanh đòn", Level.INTERMEDIATE},
                {"Walking Lunge", MuscleGroup.LEGS, "Tạ đơn", Level.BEGINNER},
                {"Overhead Press", MuscleGroup.SHOULDERS, "Thanh đòn", Level.INTERMEDIATE},
                {"Lateral Raise", MuscleGroup.SHOULDERS, "Tạ đơn", Level.BEGINNER},
                {"Face Pull", MuscleGroup.SHOULDERS, "Máy cáp, dây", Level.BEGINNER},
                {"Barbell Curl", MuscleGroup.ARMS, "Thanh đòn", Level.BEGINNER},
                {"Triceps Pushdown", MuscleGroup.ARMS, "Máy cáp", Level.BEGINNER},
                {"Plank", MuscleGroup.CORE, "Thảm", Level.BEGINNER},
                {"Hanging Leg Raise", MuscleGroup.CORE, "Xà đơn", Level.INTERMEDIATE},
                {"Chạy bộ máy", MuscleGroup.CARDIO, "Máy chạy", Level.BEGINNER},
                {"Đạp xe tại chỗ", MuscleGroup.CARDIO, "Xe đạp tập", Level.BEGINNER},
                {"Burpee", MuscleGroup.FULL_BODY, "Không", Level.INTERMEDIATE},
                {"Kettlebell Swing", MuscleGroup.FULL_BODY, "Kettlebell", Level.INTERMEDIATE}};
        for (Object[] r : exData) {
            // Flyway V3 đã tạo thư viện bài tập có nguồn → dùng lại, không tạo bản trùng tên
            var existing = exerciseRepository.findFirstByNameIgnoreCase((String) r[0]);
            if (existing.isPresent()) {
                ex.add(existing.get());
                continue;
            }
            Exercise e = new Exercise();
            e.setName((String) r[0]);
            e.setMuscleGroup((MuscleGroup) r[1]);
            e.setEquipment((String) r[2]);
            e.setLevel((Level) r[3]);
            e.setDescription("Bài " + ((String) r[0]).toLowerCase() + " cho nhóm cơ " + label((MuscleGroup) r[1]) + ".");
            e.setInstructions("1. Khởi động kỹ khớp liên quan.\n2. Giữ lưng thẳng, siết core.\n3. Thực hiện chậm, kiểm soát ở pha hạ.\n4. Thở ra khi dùng lực.");
            e.setCreatedBy(trainers[0].getUser());
            ex.add(exerciseRepository.save(e));
        }

        // ---------- Hội viên, gói, giáo án, lịch, cân nặng ----------
        MembershipPlan[] chainPlans = {p3, p1, p6, p1, pt, p3};
        for (int i = 0; i < MEMBER_NAMES.length; i++) {
            String email = i == 0 ? DemoAccounts.MEMBER.email() : "member" + (i + 1) + "@gymflow.vn";
            boolean female = Set.of(0, 2, 4, 6, 8, 10).contains(i);
            Trainer trainer = i == 7 ? null : trainers[i % 3];
            Member m = new Member();
            m.setUser(user(email, DemoAccounts.MEMBER.password(), MEMBER_NAMES[i], String.format("09130000%02d", i + 1), Role.MEMBER));
            m.setCode(String.format("HV%05d", i + 1));
            m.setGender(female ? Gender.FEMALE : Gender.MALE);
            m.setDateOfBirth(today.minusYears(19 + (i * 3) % 22).minusDays(i * 23L));
            m.setHeightCm(female ? 155.0 + i % 4 * 3 : 166.0 + i % 4 * 4);
            m.setGoal(GOALS[i % GOALS.length]);
            m.setAddress(new String[]{"Cầu Giấy", "Đống Đa", "Hai Bà Trưng", "Thanh Xuân", "Hoàn Kiếm", "Nam Từ Liêm"}[i % 6] + ", Hà Nội");
            m.setStatus(i == 9 ? MemberStatus.PAUSED : MemberStatus.ACTIVE);
            m.setTrainer(trainer);

            // Chuỗi gói nối tiếp, xây lùi từ ngày kết thúc cuối cùng.
            LocalDate finalEnd = switch (i) {
                case 2 -> today.plusDays(3);    // sắp hết hạn
                case 5 -> today.plusDays(6);    // sắp hết hạn
                case 10 -> today.plusDays(5);   // sắp hết nhưng đã gia hạn trước
                case 11 -> today.minusDays(12); // đã hết hạn
                default -> today.plusDays(20 + i * 7L);
            };
            long historyDays = 40 + i * 13L;
            List<MemberSubscription> subs = new ArrayList<>();
            LocalDate cursor = finalEnd;
            int k = i % chainPlans.length;
            boolean first = true;
            while (true) {
                MembershipPlan pl = first && i == 0 ? p12 : first && (i == 2 || i == 5 || i == 10) ? p1 : chainPlans[k++ % chainPlans.length];
                first = false;
                LocalDate start = cursor.minusDays(pl.getDurationDays() - 1L);
                subs.add(0, sub(m, pl, start, cursor, admin));
                cursor = start.minusDays(1);
                if (!start.isAfter(today.minusDays(historyDays))) break;
            }
            m.setJoinedAt(subs.get(0).getStartDate());
            memberRepository.save(m);
            if (i == 10) subs.add(sub(m, p3, finalEnd.plusDays(1), finalEnd.plusDays(90), admin)); // gói sắp tới
            for (MemberSubscription s : subs) {
                subscriptionRepository.save(s);
            }

            // Giáo án
            boolean evenPattern = i % 2 == 0;
            int[] days = evenPattern ? new int[]{1, 3, 5} : new int[]{2, 4, 6};
            WorkoutPlan wp = null;
            if (trainer != null && i != 11) {
                if (i == 0) {
                    WorkoutPlan old = workoutPlan(m, trainer, "Giai đoạn nền tảng", "Làm quen kỹ thuật", today.minusDays(100), today.minusDays(57), PlanStatus.ARCHIVED, days, ex, 3);
                    workoutPlanRepository.save(old);
                }
                wp = workoutPlanRepository.save(workoutPlan(m, trainer, i % 2 == 0 ? "Full body 3 buổi/tuần" : "Upper / Lower + Cardio",
                        m.getGoal(), today.minusDays(56), today.plusDays(28), PlanStatus.ACTIVE, days, ex, 4 + i % 2));
            }

            // Lịch tập: 8 tuần trước → 2 tuần tới, chỉ những ngày có gói hiệu lực.
            int hour = HOURS[i / 3];
            int level = i == 0 ? 9 : 6 + i % 4; // tỉ lệ đi tập /10
            LocalDate from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(8);
            LocalDate to = today.plusWeeks(2);
            int idx = 0;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                int dow = d.getDayOfWeek().getValue();
                if (dow != days[0] && dow != days[1] && dow != days[2]) continue;
                LocalDate day = d;
                if (d.isBefore(m.getJoinedAt()) || subs.stream().noneMatch(s -> s.isEffectiveOn(day))) continue;
                if (i == 9 && d.isAfter(today.minusDays(14))) continue; // đang bảo lưu
                LocalDateTime start = d.atTime(hour, 0);
                LocalDateTime end = start.plusMinutes(60);
                WorkoutSchedule s = new WorkoutSchedule();
                s.setMember(m);
                s.setTrainer(trainer);
                s.setPlan(wp != null && !d.isBefore(wp.getStartDate()) ? wp : null);
                s.setTitle(trainer == null ? "Tự tập" : wp != null ? "Buổi " + (idx % 3 + 1) + " – " + wp.getName() : "Buổi tập với HLV");
                s.setStartTime(start);
                s.setEndTime(end);
                int roll = Math.floorMod(i * 31 + idx * 17 + d.getDayOfMonth() * 7, 10);
                if (end.isBefore(now)) {
                    if (roll == 9 && i % 4 == 0) s.setStatus(ScheduleStatus.CANCELLED);
                    else s.setStatus(roll < level ? ScheduleStatus.COMPLETED : ScheduleStatus.MISSED);
                } else {
                    s.setStatus(ScheduleStatus.SCHEDULED);
                }
                scheduleRepository.save(s);
                if (s.getStatus() == ScheduleStatus.COMPLETED) {
                    WorkoutLog l = new WorkoutLog();
                    l.setSchedule(s);
                    l.setMember(m);
                    l.setCompletedAt(end.plusMinutes(5));
                    l.setDurationMinutes(50 + roll * 2);
                    l.setCalories(300 + roll * 25 + (female ? 0 : 60));
                    l.setRating(3 + roll % 3);
                    l.setNote(roll % 4 == 0 ? "Tăng được mức tạ so với tuần trước" : null);
                    logRepository.save(l);
                }
                idx++;
            }

            // Cân nặng hằng tuần
            double base = female ? 54 + i % 5 * 2.5 : 68 + i % 5 * 3.0;
            double trend = m.getGoal().startsWith("Tăng") ? 0.2 : -0.35;
            for (int w = 9; w >= 0; w--) {
                LocalDate d = today.minusWeeks(w).minusDays(i % 3);
                if (d.isBefore(m.getJoinedAt())) continue;
                WeightRecord r = new WeightRecord();
                r.setMember(m);
                r.setRecordedDate(d);
                r.setWeightKg(Math.round((base + trend * (9 - w) + ((w * 7 + i) % 3 - 1) * 0.2) * 10) / 10.0);
                r.setBodyFat(Math.round(((female ? 27 : 20) + trend * (9 - w) * 0.6) * 10) / 10.0);
                weightRepository.save(r);
            }
        }

        // Ngày tạo đăng ký = thời điểm thu tiền, để doanh thu trải đều nhiều tháng.
        subscriptionRepository.flush();
        for (MemberSubscription s : subscriptionRepository.findAll()) {
            LocalDateTime paid = s.getStartDate().minusDays(1).atTime(9, 30);
            if (paid.isAfter(now)) paid = now.minusHours(2);
            jdbc.update("UPDATE member_subscriptions SET created_at = ? WHERE id = ?", paid, s.getId());
        }

        AuditLog a = new AuditLog();
        a.setActorEmail("system");
        a.setAction("SYSTEM_SEED");
        a.setTargetType("SYSTEM");
        a.setDetail("Khởi tạo dữ liệu mẫu: 12 hội viên, 3 HLV, 6 gói, 23 bài tập");
        a.setSuccess(true);
        auditRepository.save(a);
        log.info("Đã sinh dữ liệu mẫu. Tài khoản demo: {}", DemoAccounts.ALL.stream().map(acc -> acc.email()).toList());
    }

    // ---------- helpers ----------
    private User user(String email, String password, String name, String phone, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        return userRepository.save(u);
    }

    private Trainer trainer(User u, String specialty, int years, String certs, String bio) {
        Trainer t = new Trainer();
        t.setUser(u);
        t.setSpecialty(specialty);
        t.setExperienceYears(years);
        t.setCertificates(certs);
        t.setBio(bio);
        t.setActive(true);
        return t;
    }

    private void shift(Trainer t, int day, LocalTime start, LocalTime end) {
        TrainerShift s = new TrainerShift();
        s.setTrainer(t);
        s.setDayOfWeek(day);
        s.setStartTime(start);
        s.setEndTime(end);
        t.getShifts().add(s);
    }

    private MembershipPlan plan(String name, int days, long price, String desc, String benefits, boolean active) {
        MembershipPlan p = new MembershipPlan();
        p.setName(name);
        p.setDurationDays(days);
        p.setPrice(price);
        p.setDescription(desc);
        p.setBenefits(benefits);
        p.setActive(active);
        return planRepository.save(p);
    }

    private MemberSubscription sub(Member m, MembershipPlan p, LocalDate start, LocalDate end, User by) {
        MemberSubscription s = new MemberSubscription();
        s.setMember(m);
        s.setPlan(p);
        s.setStartDate(start);
        s.setEndDate(end);
        s.setPricePaid(p.getPrice());
        s.setCreatedBy(by);
        return s;
    }

    private WorkoutPlan workoutPlan(Member m, Trainer t, String name, String goal, LocalDate start, LocalDate end,
                                    PlanStatus status, int[] days, List<Exercise> ex, int perDay) {
        WorkoutPlan p = new WorkoutPlan();
        p.setMember(m);
        p.setTrainer(t);
        p.setName(name);
        p.setGoal(goal);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(status);
        p.setNote("Khởi động 10 phút trước mỗi buổi, giãn cơ 5 phút sau buổi.");
        // Mỗi ngày tập lấy một nhóm bài khác nhau, không lặp bài trong cùng ngày.
        int[][] dayPools = {{0, 5, 8, 13, 17, 19}, {4, 1, 10, 14, 16, 18}, {9, 6, 2, 12, 15, 21}};
        for (int d = 0; d < days.length; d++) {
            for (int j = 0; j < perDay; j++) {
                Exercise e = ex.get(dayPools[d][j]);
                WorkoutPlanItem it = new WorkoutPlanItem();
                it.setPlan(p);
                it.setExercise(e);
                it.setDayOfWeek(days[d]);
                it.setSortOrder(j);
                boolean cardio = e.getMuscleGroup() == MuscleGroup.CARDIO;
                it.setSets(cardio ? 1 : 3 + j % 2);
                it.setReps(cardio ? "1" : e.getMuscleGroup() == MuscleGroup.CORE ? "45s" : j == 0 ? "6-8" : "10-12");
                it.setWeightKg(cardio || e.getEquipment().equals("Không") || e.getMuscleGroup() == MuscleGroup.CORE ? null : 10.0 + j * 7.5);
                it.setRestSeconds(cardio ? null : j == 0 ? 120 : 75);
                it.setDurationMinutes(cardio ? 15 : null);
                p.getItems().add(it);
            }
        }
        return p;
    }

    private static String label(MuscleGroup g) {
        return switch (g) {
            case CHEST -> "ngực";
            case BACK -> "lưng";
            case LEGS -> "chân";
            case SHOULDERS -> "vai";
            case ARMS -> "tay";
            case CORE -> "bụng – core";
            case CARDIO -> "tim mạch";
            case FULL_BODY -> "toàn thân";
        };
    }
}
