package com.gymflow.dto;

import com.gymflow.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Toàn bộ request/response của API, gom một chỗ để FE (src/api/types.ts) đối chiếu 1-1. */
public final class Dtos {
    private Dtos() {
    }

    private static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    private static final String PASSWORD_MSG = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số";
    private static final String PHONE_RULE = "^$|^(0|\\+84)\\d{9,10}$";
    private static final String PHONE_MSG = "Số điện thoại không hợp lệ";

    // ===================== Auth =====================
    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120, message = "Email tối đa 120 ký tự") String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    public record MeResponse(Long id, String email, String fullName, String phone, Role role,
                             Long memberId, Long trainerId, LocalDateTime lastLoginAt) {
    }

    public record LoginResponse(String token, long expiresInMinutes, MeResponse user) {
    }

    /**
     * Hội viên tự đăng ký. Không có trường vai trò: server luôn gán MEMBER, nên gửi thêm "role" cũng bị bỏ qua.
     * Mật khẩu xác nhận được kiểm tra lại ở service (FE đã kiểm tra trước).
     */
    public record RegisterRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự") String fullName,
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120, message = "Email tối đa 120 ký tự") String email,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @NotBlank(message = "Vui lòng nhập mật khẩu") @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String password,
            @NotBlank(message = "Vui lòng nhập lại mật khẩu") String confirmPassword,
            @AssertTrue(message = "Bạn cần đồng ý Điều khoản sử dụng và Chính sách bảo mật") boolean acceptTerms) {
    }

    public record RegisterResponse(String message, String email) {
    }

    /** Tài khoản demo chỉ trả về khi bật app.demo.quick-login và tài khoản thật sự tồn tại, đang hoạt động. */
    public record DemoAccount(String label, Role role, String email, String password) {
    }

    public record AuthOptions(boolean registrationEnabled, List<DemoAccount> demoAccounts) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String newPassword) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120, message = "Email tối đa 120 ký tự") String email) {
    }

    /** resetToken chỉ trả về khi bật chế độ demo (app.reset.expose-token=true) vì chưa tích hợp gửi email. */
    public record ForgotPasswordResponse(String message, String resetToken, int validMinutes) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Vui lòng nhập mã khôi phục") String token,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String newPassword) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone) {
    }

    // ===================== Member =====================
    public record MemberCreateRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120, message = "Email tối đa 120 ký tự") String email,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @Pattern(regexp = "^$|" + PASSWORD_RULE, message = PASSWORD_MSG) String password,
            @Past(message = "Ngày sinh phải ở quá khứ") LocalDate dateOfBirth,
            Gender gender,
            @DecimalMin(value = "80", message = "Chiều cao từ 80 cm") @DecimalMax(value = "250", message = "Chiều cao tối đa 250 cm") Double heightCm,
            @Size(max = 255) String goal,
            @Size(max = 255) String address,
            Long trainerId,
            @Size(max = 255) String note) {
    }

    public record MemberUpdateRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @Past(message = "Ngày sinh phải ở quá khứ") LocalDate dateOfBirth,
            Gender gender,
            @DecimalMin(value = "80", message = "Chiều cao từ 80 cm") @DecimalMax(value = "250", message = "Chiều cao tối đa 250 cm") Double heightCm,
            @Size(max = 255) String goal,
            @Size(max = 255) String address,
            Long trainerId,
            @Size(max = 255) String note) {
    }

    /** Hội viên tự sửa: không được đổi HLV hay ghi chú nội bộ. */
    public record MemberSelfUpdateRequest(
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @Past(message = "Ngày sinh phải ở quá khứ") LocalDate dateOfBirth,
            Gender gender,
            @DecimalMin(value = "80", message = "Chiều cao từ 80 cm") @DecimalMax(value = "250", message = "Chiều cao tối đa 250 cm") Double heightCm,
            @Size(max = 255) String goal,
            @Size(max = 255) String address) {
    }

    public record MemberStatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") MemberStatus status) {
    }

    public record AssignTrainerRequest(Long trainerId) {
    }

    public record MemberResponse(Long id, Long userId, String code, String fullName, String email, String phone,
                                 LocalDate dateOfBirth, Gender gender, Double heightCm, String goal, String address,
                                 MemberStatus status, UserStatus accountStatus, Long trainerId, String trainerName,
                                 LocalDate joinedAt, String note,
                                 String currentPlanName, SubscriptionState subscriptionState,
                                 LocalDate subscriptionEndDate, Long daysLeft) {
    }

    public record MemberCreatedResponse(MemberResponse member, String temporaryPassword) {
    }

    public record MemberDetailResponse(MemberResponse member, List<SubscriptionResponse> subscriptions,
                                       List<WorkoutPlanSummary> plans, WeightResponse latestWeight,
                                       long totalSessions, long completedSessions) {
    }

    // ===================== Trainer =====================
    public record TrainerCreateRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120, message = "Email tối đa 120 ký tự") String email,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @Pattern(regexp = "^$|" + PASSWORD_RULE, message = PASSWORD_MSG) String password,
            @Size(max = 120) String specialty,
            @Min(value = 0, message = "Kinh nghiệm không âm") @Max(value = 60, message = "Kinh nghiệm tối đa 60 năm") Integer experienceYears,
            @Size(max = 255) String certificates,
            @Size(max = 1000) String bio) {
    }

    public record TrainerUpdateRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @Size(max = 120) String specialty,
            @Min(value = 0, message = "Kinh nghiệm không âm") @Max(value = 60, message = "Kinh nghiệm tối đa 60 năm") Integer experienceYears,
            @Size(max = 255) String certificates,
            @Size(max = 1000) String bio) {
    }

    public record ActiveRequest(@NotNull Boolean active) {
    }

    public record ShiftRequest(
            @NotNull @Min(value = 1, message = "Thứ từ 1 (T2) đến 7 (CN)") @Max(value = 7, message = "Thứ từ 1 (T2) đến 7 (CN)") Integer dayOfWeek,
            @NotNull(message = "Thiếu giờ bắt đầu") LocalTime startTime,
            @NotNull(message = "Thiếu giờ kết thúc") LocalTime endTime) {
    }

    public record ShiftsRequest(@NotNull @Valid List<ShiftRequest> shifts) {
    }

    public record ShiftResponse(Long id, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }

    public record TrainerResponse(Long id, Long userId, String fullName, String email, String phone, String specialty,
                                  Integer experienceYears, String certificates, String bio, boolean active,
                                  UserStatus accountStatus, long memberCount, List<ShiftResponse> shifts) {
    }

    public record TrainerCreatedResponse(TrainerResponse trainer, String temporaryPassword) {
    }

    // ===================== Membership plan =====================
    public record PlanRequest(
            @NotBlank(message = "Vui lòng nhập tên gói") @Size(max = 100) String name,
            @Size(max = 500) String description,
            @NotNull(message = "Vui lòng nhập giá") @Min(value = 0, message = "Giá không âm") Long price,
            @NotNull(message = "Vui lòng nhập thời hạn") @Min(value = 1, message = "Thời hạn tối thiểu 1 ngày") @Max(value = 1095, message = "Thời hạn tối đa 3 năm") Integer durationDays,
            @Size(max = 1000) String benefits) {
    }

    public record PlanResponse(Long id, String name, String description, Long price, Integer durationDays,
                               String benefits, boolean active, long currentSubscriptions) {
    }

    // ===================== Subscription =====================
    public record SubscriptionRequest(
            @NotNull(message = "Vui lòng chọn hội viên") Long memberId,
            @NotNull(message = "Vui lòng chọn gói tập") Long planId,
            LocalDate startDate,
            @Min(value = 0, message = "Số tiền không âm") Long pricePaid,
            @Size(max = 255) String note) {
    }

    public record CancelRequest(@Size(max = 200) String reason) {
    }

    public record SubscriptionPreview(LocalDate startDate, LocalDate endDate, Long price, boolean chained) {
    }

    public record SubscriptionResponse(Long id, Long memberId, String memberName, String memberCode,
                                       Long planId, String planName, LocalDate startDate, LocalDate endDate,
                                       Long pricePaid, SubscriptionState state, long daysLeft, boolean cancelled,
                                       LocalDateTime cancelledAt, String note, LocalDateTime createdAt, String createdByName) {
    }

    // ===================== Exercise =====================
    public record ExerciseRequest(
            @NotBlank(message = "Vui lòng nhập tên bài tập") @Size(max = 120) String name,
            @NotNull(message = "Vui lòng chọn nhóm cơ") MuscleGroup muscleGroup,
            @Size(max = 60) String equipment,
            @NotNull(message = "Vui lòng chọn cấp độ") Level level,
            @Size(max = 1000) String description,
            @Size(max = 2000) String instructions,
            @Pattern(regexp = "^$|^https?://.+", message = "Link video phải bắt đầu bằng http(s)://") @Size(max = 255) String videoUrl,
            @Size(max = 120) String nameEn,
            @Size(max = 255) String aliases,
            @Size(max = 255) String secondaryMuscles,
            ExerciseLocation location,
            List<ExerciseGoal> goals,
            @Size(max = 500) String breathing,
            @Size(max = 2000) String commonMistakes,
            @Size(max = 2000) String safetyNotes,
            @Size(max = 2000) String variations,
            @Size(max = 255) String suggestedDose,
            ContentOrigin contentOrigin,
            /** null = giữ nguyên nguồn đang gắn; danh sách rỗng = bỏ hết nguồn */
            List<Long> sourceIds) {
    }

    public record HiddenRequest(@NotNull Boolean hidden) {
    }

    /** Chỉ ADMIN. NEEDS_REVIEW bắt buộc kèm ghi chú để người biên soạn biết cần sửa gì. */
    public record ReviewRequest(
            @NotNull(message = "Vui lòng chọn trạng thái duyệt") ReviewStatus status,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note) {
    }

    public record SourceResponse(Long id, String code, String publisher, String title, String url, String license,
                                 String licenseUrl, boolean licenseVerified, String attribution,
                                 LocalDate publisherReviewed, LocalDate accessedAt) {
    }

    public record ExerciseResponse(Long id, String name, MuscleGroup muscleGroup, String equipment, Level level,
                                   String description, String instructions, String videoUrl, boolean hidden,
                                   long usageCount, String createdByName,
                                   String nameEn, String aliases, String secondaryMuscles, ExerciseLocation location,
                                   List<ExerciseGoal> goals, String breathing, String commonMistakes, String safetyNotes,
                                   String variations, String suggestedDose, ContentOrigin contentOrigin,
                                   ReviewStatus reviewStatus, String reviewNote, String reviewedByName,
                                   LocalDateTime reviewedAt, LocalDateTime updatedAt, List<SourceResponse> sources) {
    }

    // ===================== Knowledge base =====================
    public record ArticleRequest(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
            @NotBlank(message = "Vui lòng nhập đường dẫn")
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Đường dẫn chỉ gồm chữ thường không dấu, số và dấu gạch ngang")
            @Size(max = 120) String slug,
            @NotNull(message = "Vui lòng chọn chủ đề") ArticleCategory category,
            @NotBlank(message = "Vui lòng nhập tóm tắt") @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự") String summary,
            @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 20000, message = "Nội dung tối đa 20.000 ký tự") String body,
            ContentOrigin contentOrigin,
            List<Long> sourceIds) {
    }

    /** SUBMIT (gửi duyệt) · APPROVE (xuất bản) · REJECT (trả về bản nháp) · ARCHIVE (gỡ) · RESTORE (khôi phục về nháp) */
    public enum ArticleAction { SUBMIT, APPROVE, REJECT, ARCHIVE, RESTORE }

    public record ArticleActionRequest(
            @NotNull(message = "Vui lòng chọn thao tác") ArticleAction action,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note) {
    }

    /** Bản rút gọn cho danh sách công khai (không kèm thân bài). */
    public record ArticleSummary(Long id, String slug, String title, ArticleCategory category, String summary,
                                 LocalDateTime publishedAt) {
    }

    public record ArticleResponse(Long id, String slug, String title, ArticleCategory category, String summary,
                                  String body, ContentOrigin contentOrigin, ArticleStatus status, String reviewNote,
                                  String authorName, Long authorId, String reviewedByName, LocalDateTime reviewedAt,
                                  LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                                  List<SourceResponse> sources) {
    }

    // ===================== Workout plan =====================
    public record PlanItemRequest(
            @NotNull(message = "Vui lòng chọn bài tập") Long exerciseId,
            @NotNull @Min(value = 1, message = "Thứ từ 1 đến 7") @Max(value = 7, message = "Thứ từ 1 đến 7") Integer dayOfWeek,
            Integer sortOrder,
            @NotNull(message = "Nhập số hiệp") @Min(value = 1, message = "Tối thiểu 1 hiệp") @Max(value = 20, message = "Tối đa 20 hiệp") Integer sets,
            @NotBlank(message = "Nhập số lần (VD: 10-12)") @Size(max = 20) String reps,
            @DecimalMin(value = "0", message = "Mức tạ không âm") Double weightKg,
            @Min(value = 0, message = "Nghỉ không âm") @Max(value = 900, message = "Nghỉ tối đa 900 giây") Integer restSeconds,
            @Min(value = 1, message = "Tối thiểu 1 phút") @Max(value = 300, message = "Tối đa 300 phút") Integer durationMinutes,
            @Size(max = 255) String note) {
    }

    public record WorkoutPlanRequest(
            @NotNull(message = "Vui lòng chọn hội viên") Long memberId,
            @NotBlank(message = "Vui lòng nhập tên giáo án") @Size(max = 120) String name,
            @Size(max = 255) String goal,
            @NotNull(message = "Chọn ngày bắt đầu") LocalDate startDate,
            @NotNull(message = "Chọn ngày kết thúc") LocalDate endDate,
            @Size(max = 1000) String note,
            @NotNull @Size(max = 100, message = "Tối đa 100 bài tập") @Valid List<PlanItemRequest> items) {
    }

    public record PlanStatusRequest(@NotNull PlanStatus status) {
    }

    public record PlanItemResponse(Long id, Long exerciseId, String exerciseName, MuscleGroup muscleGroup,
                                   String equipment, Integer dayOfWeek, Integer sortOrder, Integer sets, String reps,
                                   Double weightKg, Integer restSeconds, Integer durationMinutes, String note) {
    }

    public record WorkoutPlanSummary(Long id, Long memberId, String memberName, Long trainerId, String trainerName,
                                     String name, String goal, LocalDate startDate, LocalDate endDate,
                                     PlanStatus status, int itemCount, int trainingDays, LocalDateTime createdAt) {
    }

    public record WorkoutPlanResponse(WorkoutPlanSummary summary, String note, List<PlanItemResponse> items) {
    }

    // ===================== Schedule =====================
    public record ScheduleRequest(
            @NotNull(message = "Vui lòng chọn hội viên") Long memberId,
            Long trainerId,
            Long planId,
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 120, message = "Tiêu đề tối đa 120 ký tự") String title,
            @NotNull(message = "Chọn giờ bắt đầu") LocalDateTime startTime,
            @NotNull(message = "Chọn giờ kết thúc") LocalDateTime endTime,
            @Size(max = 255) String note,
            @Min(value = 0, message = "Số tuần lặp không âm") @Max(value = 12, message = "Lặp tối đa 12 tuần") Integer repeatWeeks) {
    }

    public record ScheduleUpdateRequest(
            Long trainerId,
            Long planId,
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 120, message = "Tiêu đề tối đa 120 ký tự") String title,
            @NotNull(message = "Chọn giờ bắt đầu") LocalDateTime startTime,
            @NotNull(message = "Chọn giờ kết thúc") LocalDateTime endTime,
            @Size(max = 255) String note) {
    }

    public record CompleteRequest(
            @Min(value = 1, message = "Tối thiểu 1 phút") @Max(value = 600, message = "Tối đa 600 phút") Integer durationMinutes,
            @Min(value = 0, message = "Calo không âm") @Max(value = 5000, message = "Tối đa 5000 kcal") Integer calories,
            @Min(value = 1, message = "Đánh giá từ 1 đến 5") @Max(value = 5, message = "Đánh giá từ 1 đến 5") Integer rating,
            @Size(max = 255) String note) {
    }

    public record WorkoutLogResponse(Long id, LocalDateTime completedAt, Integer durationMinutes, Integer calories,
                                     Integer rating, String note) {
    }

    public record ScheduleResponse(Long id, Long memberId, String memberName, Long trainerId, String trainerName,
                                   Long planId, String planName, String title, LocalDateTime startTime,
                                   LocalDateTime endTime, ScheduleStatus status, String note, WorkoutLogResponse log) {
    }

    public record ScheduleCreatedResponse(List<ScheduleResponse> created, List<String> skipped) {
    }

    // ===================== Weight & progress =====================
    public record WeightRequest(
            @NotNull(message = "Chọn ngày") @PastOrPresent(message = "Không ghi cho ngày tương lai") LocalDate recordedDate,
            @NotNull(message = "Nhập cân nặng") @DecimalMin(value = "20", message = "Cân nặng từ 20 kg") @DecimalMax(value = "300", message = "Cân nặng tối đa 300 kg") Double weightKg,
            @DecimalMin(value = "2", message = "% mỡ từ 2") @DecimalMax(value = "70", message = "% mỡ tối đa 70") Double bodyFat,
            @Size(max = 255) String note) {
    }

    public record WeightResponse(Long id, LocalDate recordedDate, Double weightKg, Double bodyFat, String note) {
    }

    public record WeekAttendance(LocalDate weekStart, long scheduled, long completed, long missed) {
    }

    public record ProgressResponse(Long memberId, Double heightCm, Double currentWeight, Double startWeight,
                                   Double weightChange, Double bodyFat, Double bmi, String bmiCategory,
                                   List<WeightResponse> weights, List<WeekAttendance> attendance,
                                   Double attendanceRate, int streakWeeks, long totalSessions,
                                   long totalMinutes, long totalCalories, Double avgRating) {
    }

    // ===================== Dashboards =====================
    public record LabelValue(String label, long value) {
    }

    public record AdminDashboard(long totalMembers, long activeMembers, long newMembersThisMonth,
                                 long membersWithActivePackage, long expiringSoon, long revenueThisMonth,
                                 long revenuePrevMonth, long activeTrainers, long sessionsToday,
                                 List<LabelValue> revenueByMonth, List<LabelValue> planDistribution,
                                 List<SubscriptionResponse> expiring, List<AuditResponse> recentActivity) {
    }

    public record TrainerDashboard(long memberCount, long sessionsToday, long sessionsThisWeek,
                                   long completedThisWeek, long missedThisWeek, Double completionRate,
                                   long activePlans, List<ScheduleResponse> todaySessions,
                                   List<SubscriptionResponse> expiringMembers, List<LabelValue> weekLoad) {
    }

    public record MemberDashboard(MemberResponse member, SubscriptionResponse currentSubscription,
                                  ScheduleResponse nextSession, List<ScheduleResponse> weekSessions,
                                  long completedThisMonth, int streakWeeks, WeightResponse latestWeight,
                                  Double bmi, String bmiCategory, WorkoutPlanSummary currentPlan) {
    }

    // ===================== Reports =====================
    public record TrainerAttendance(Long trainerId, String trainerName, long total, long completed, long missed) {
    }

    public record ReportResponse(LocalDate from, LocalDate to, long revenueTotal, long subscriptionCount,
                                 List<LabelValue> revenueByMonth, List<LabelValue> revenueByPlan,
                                 long scheduled, long completed, long missed, long cancelled, Double attendanceRate,
                                 List<TrainerAttendance> byTrainer, List<LabelValue> membersByStatus,
                                 List<LabelValue> membersByGender, List<LabelValue> newMembersByMonth) {
    }

    // ===================== Users & audit =====================
    public record UserResponse(Long id, String email, String fullName, String phone, Role role, UserStatus status,
                               LocalDateTime lastLoginAt, LocalDateTime createdAt) {
    }

    public record UserStatusRequest(@NotNull UserStatus status) {
    }

    public record AdminCreateRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120, message = "Email tối đa 120 ký tự") String email,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @Pattern(regexp = PHONE_RULE, message = PHONE_MSG) String phone,
            @Pattern(regexp = "^$|" + PASSWORD_RULE, message = PASSWORD_MSG) String password) {
    }

    public record UserCreatedResponse(UserResponse user, String temporaryPassword) {
    }

    public record TempPasswordResponse(String temporaryPassword) {
    }

    public record AuditResponse(Long id, Long actorId, String actorEmail, String action, String targetType,
                                String targetId, String detail, boolean success, String ip, LocalDateTime createdAt) {
    }

    // ===================== Notifications =====================
    public record NotificationResponse(String id, String level, String title, String message, String link,
                                       LocalDateTime at) {
    }

    public record MessageResponse(String message) {
    }
}
