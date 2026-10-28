-- =====================================================================
-- GymFlow · V1 · Lược đồ CSDL (MySQL 8.4, InnoDB, utf8mb4)
-- Người phụ trách: Database Developer (Nguyễn Thế Kiên)
-- Nguyên tắc: ràng buộc nằm ở CSDL (PK/FK/UNIQUE/CHECK),
-- backend kiểm tra lại để trả thông báo thân thiện.
--
-- Khác bản PostgreSQL trước đây:
--  * MySQL không có EXCLUDE USING gist → chống trùng gói / trùng giờ được kiểm tra ở service,
--    có khoá dòng hội viên/HLV (SELECT … FOR UPDATE) để hai yêu cầu đồng thời không lọt.
--  * Không có partial index → dùng index thường.
--  * Collation mặc định utf8mb4_0900_ai_ci không phân biệt dấu ("Gói" = "Goi"): tiện cho tìm kiếm.
--    Các cột tên có UNIQUE dùng utf8mb4_0900_as_ci để "Gói Cơ bản" và "Goi Co ban" là hai tên khác nhau.
--  * Tên CHECK constraint trong MySQL phải duy nhất toàn schema → giữ tiền tố theo bảng.
-- =====================================================================

-- ---------- Tài khoản ----------
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(120) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at   DATETIME(6),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'TRAINER', 'MEMBER')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT ck_users_email_format CHECK (REGEXP_LIKE(email, '^[^@[:space:]]+@[^@[:space:]]+[.][^@[:space:]]+$'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(100) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX ix_reset_tokens_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Huấn luyện viên ----------
CREATE TABLE trainers (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    specialty         VARCHAR(150),
    experience_years  INT          NOT NULL DEFAULT 0,
    certificates      VARCHAR(255),
    bio               TEXT,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_trainers_user UNIQUE (user_id),
    CONSTRAINT fk_trainers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_trainers_exp CHECK (experience_years BETWEEN 0 AND 60)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE trainer_shifts (
    id           BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trainer_id   BIGINT   NOT NULL,
    day_of_week  SMALLINT NOT NULL,
    start_time   TIME     NOT NULL,
    end_time     TIME     NOT NULL,
    CONSTRAINT fk_shifts_trainer FOREIGN KEY (trainer_id) REFERENCES trainers (id) ON DELETE CASCADE,
    CONSTRAINT ck_shift_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_shift_time CHECK (end_time > start_time),
    INDEX ix_shifts_trainer (trainer_id, day_of_week)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Hội viên ----------
CREATE TABLE members (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    code           VARCHAR(20)  NOT NULL,
    date_of_birth  DATE,
    gender         VARCHAR(10),
    height_cm      DECIMAL(5, 1),
    goal           VARCHAR(255),
    address        VARCHAR(255),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    trainer_id     BIGINT,
    joined_at      DATE         NOT NULL DEFAULT (CURRENT_DATE),
    note           TEXT,
    CONSTRAINT uq_members_user UNIQUE (user_id),
    CONSTRAINT uq_members_code UNIQUE (code),
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_members_trainer FOREIGN KEY (trainer_id) REFERENCES trainers (id) ON DELETE SET NULL,
    CONSTRAINT ck_members_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_members_height CHECK (height_cm IS NULL OR height_cm BETWEEN 80 AND 250),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'PAUSED', 'INACTIVE')),
    CONSTRAINT ck_members_dob CHECK (date_of_birth IS NULL OR date_of_birth > DATE '1900-01-01'),
    INDEX ix_members_trainer (trainer_id),
    INDEX ix_members_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Gói tập & đăng ký ----------
CREATE TABLE membership_plans (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) COLLATE utf8mb4_0900_as_ci NOT NULL,
    description    TEXT,
    price          BIGINT       NOT NULL,
    duration_days  INT          NOT NULL,
    benefits       TEXT,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_plans_name UNIQUE (name),
    CONSTRAINT ck_plans_price CHECK (price >= 0),
    CONSTRAINT ck_plans_duration CHECK (duration_days BETWEEN 1 AND 1095)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE member_subscriptions (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT       NOT NULL,
    plan_id       BIGINT       NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    price_paid    BIGINT       NOT NULL,
    cancelled     BOOLEAN      NOT NULL DEFAULT FALSE,
    cancelled_at  DATETIME(6),
    note          VARCHAR(255),
    created_by    BIGINT,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sub_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES membership_plans (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_sub_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_sub_price CHECK (price_paid >= 0),
    -- Chống 2 gói (chưa huỷ) chồng thời gian: SubscriptionService.create (khoá dòng hội viên + existsOverlap)
    INDEX ix_sub_member (member_id, end_date),
    INDEX ix_sub_end (cancelled, end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Bài tập & kế hoạch ----------
CREATE TABLE exercises (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(120) COLLATE utf8mb4_0900_as_ci NOT NULL,
    muscle_group  VARCHAR(30)  NOT NULL,
    equipment     VARCHAR(60),
    level         VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    description   TEXT,
    instructions  TEXT,
    video_url     VARCHAR(255),
    hidden        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by    BIGINT,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_exercises_name UNIQUE (name),
    CONSTRAINT fk_ex_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_ex_group CHECK (muscle_group IN ('CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE', 'CARDIO', 'FULL_BODY')),
    CONSTRAINT ck_ex_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workout_plans (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    trainer_id  BIGINT,
    name        VARCHAR(120) NOT NULL,
    goal        VARCHAR(255),
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    note        TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wp_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_wp_trainer FOREIGN KEY (trainer_id) REFERENCES trainers (id) ON DELETE SET NULL,
    CONSTRAINT ck_wp_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_wp_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED')),
    INDEX ix_wp_member (member_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workout_plan_items (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_id           BIGINT       NOT NULL,
    exercise_id       BIGINT       NOT NULL,
    day_of_week       SMALLINT     NOT NULL,
    sort_order        INT          NOT NULL DEFAULT 0,
    sets              INT          NOT NULL,
    reps              VARCHAR(20)  NOT NULL,
    weight_kg         DECIMAL(6, 1),
    rest_seconds      INT,
    duration_minutes  INT,
    note              VARCHAR(255),
    CONSTRAINT fk_wpi_plan FOREIGN KEY (plan_id) REFERENCES workout_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_wpi_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE RESTRICT,
    CONSTRAINT ck_wpi_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_wpi_sets CHECK (sets BETWEEN 1 AND 20),
    CONSTRAINT ck_wpi_weight CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT ck_wpi_rest CHECK (rest_seconds IS NULL OR rest_seconds BETWEEN 0 AND 900),
    CONSTRAINT ck_wpi_duration CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 1 AND 300),
    INDEX ix_wpi_plan (plan_id, day_of_week, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Lịch tập & nhật ký ----------
CREATE TABLE workout_schedules (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    trainer_id  BIGINT,
    plan_id     BIGINT,
    title       VARCHAR(120) NOT NULL,
    start_time  DATETIME(6)  NOT NULL,
    end_time    DATETIME(6)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    note        VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ws_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_ws_trainer FOREIGN KEY (trainer_id) REFERENCES trainers (id) ON DELETE SET NULL,
    CONSTRAINT fk_ws_plan FOREIGN KEY (plan_id) REFERENCES workout_plans (id) ON DELETE SET NULL,
    CONSTRAINT ck_ws_time CHECK (end_time > start_time),
    CONSTRAINT ck_ws_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'MISSED', 'CANCELLED')),
    -- Chống trùng giờ HLV / hội viên: ScheduleService.conflict (khoá dòng hội viên rồi HLV + memberBusy/trainerBusy)
    INDEX ix_ws_member_time (member_id, start_time),
    INDEX ix_ws_trainer_time (trainer_id, start_time),
    INDEX ix_ws_status_time (status, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workout_logs (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    schedule_id       BIGINT,
    member_id         BIGINT       NOT NULL,
    completed_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    duration_minutes  INT,
    calories          INT,
    rating            SMALLINT,
    note              VARCHAR(255),
    CONSTRAINT uq_logs_schedule UNIQUE (schedule_id),
    CONSTRAINT fk_logs_schedule FOREIGN KEY (schedule_id) REFERENCES workout_schedules (id) ON DELETE SET NULL,
    CONSTRAINT fk_logs_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT ck_logs_duration CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 1 AND 600),
    CONSTRAINT ck_logs_calories CHECK (calories IS NULL OR calories BETWEEN 0 AND 5000),
    CONSTRAINT ck_logs_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    INDEX ix_logs_member (member_id, completed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Theo dõi cân nặng ----------
CREATE TABLE weight_records (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT        NOT NULL,
    recorded_date  DATE          NOT NULL,
    weight_kg      DECIMAL(5, 1) NOT NULL,
    body_fat       DECIMAL(4, 1),
    note           VARCHAR(255),
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_weight_member_day UNIQUE (member_id, recorded_date),
    CONSTRAINT fk_weight_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT ck_weight_range CHECK (weight_kg BETWEEN 20 AND 300),
    CONSTRAINT ck_body_fat_range CHECK (body_fat IS NULL OR body_fat BETWEEN 2 AND 70)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- Nhật ký hệ thống ----------
CREATE TABLE audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id     BIGINT,
    actor_email  VARCHAR(120),
    action       VARCHAR(60)  NOT NULL,
    target_type  VARCHAR(40),
    target_id    VARCHAR(40),
    detail       VARCHAR(500),
    success      BOOLEAN      NOT NULL DEFAULT TRUE,
    ip           VARCHAR(45),
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX ix_audit_created (created_at DESC),
    INDEX ix_audit_actor (actor_id, created_at DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------- View báo cáo ----------
-- Trạng thái gói tính theo ngày hiện tại, không lưu cứng
CREATE VIEW v_subscription_status AS
SELECT s.*,
       CASE
           WHEN s.cancelled THEN 'CANCELLED'
           WHEN CURRENT_DATE < s.start_date THEN 'UPCOMING'
           WHEN CURRENT_DATE > s.end_date THEN 'EXPIRED'
           WHEN DATEDIFF(s.end_date, CURRENT_DATE) <= 7 THEN 'EXPIRING'
           ELSE 'ACTIVE'
       END AS computed_status,
       DATEDIFF(s.end_date, CURRENT_DATE) AS days_left
FROM member_subscriptions s;
