-- =====================================================================
-- GymFlow – V2: thư viện bài tập có nguồn + kho kiến thức (MySQL 8.4)
--  * Mọi nội dung sức khoẻ đều có trạng thái duyệt; chỉ ADMIN được duyệt.
--  * content_sources lưu nguồn tham khảo kèm giấy phép. license_verified = FALSE
--    nghĩa là nhóm CHƯA đối chiếu được điều khoản giấy phép trên trang gốc.
--  * Không xoá/ghi đè dữ liệu cũ: chỉ thêm cột (có giá trị mặc định) và bảng mới.
-- =====================================================================

CREATE TABLE content_sources (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(40)   NOT NULL,
    publisher          VARCHAR(120)  NOT NULL,
    title              VARCHAR(255)  NOT NULL,
    url                VARCHAR(500)  NOT NULL,
    license            VARCHAR(120),
    license_url        VARCHAR(500),
    license_verified   BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Câu ghi công bắt buộc theo giấy phép (vd. OGL v3.0), hiển thị nguyên văn
    attribution        VARCHAR(500),
    -- "Last reviewed" do đơn vị phát hành ghi trên trang; NULL nếu trang không ghi
    publisher_reviewed DATE,
    accessed_at        DATE          NOT NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_source_code UNIQUE (code),
    CONSTRAINT ck_source_url CHECK (url LIKE 'https://%')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE exercises
    ADD COLUMN name_en           VARCHAR(120),
    ADD COLUMN aliases           VARCHAR(255),
    ADD COLUMN secondary_muscles VARCHAR(255),
    -- GYM | HOME | ANY
    ADD COLUMN location          VARCHAR(10),
    -- Danh sách mục tiêu cách nhau bởi dấu phẩy: STRENGTH, ENDURANCE, MOBILITY, BALANCE, FAT_LOSS
    ADD COLUMN goals             VARCHAR(120),
    ADD COLUMN breathing         VARCHAR(500),
    ADD COLUMN common_mistakes   TEXT,
    ADD COLUMN safety_notes      TEXT,
    ADD COLUMN variations        TEXT,
    ADD COLUMN suggested_dose    VARCHAR(255),
    -- SELF_AUTHORED: nhóm tự viết · ADAPTED: biên soạn lại từ nguồn mở (không ghi là nội dung của nguồn)
    ADD COLUMN content_origin    VARCHAR(20)  NOT NULL DEFAULT 'SELF_AUTHORED',
    ADD COLUMN review_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN review_note       VARCHAR(500),
    ADD COLUMN reviewed_by       BIGINT,
    ADD COLUMN reviewed_at       DATETIME(6),
    ADD COLUMN updated_at        DATETIME(6),
    ADD CONSTRAINT fk_ex_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_ex_location CHECK (location IS NULL OR location IN ('GYM', 'HOME', 'ANY')),
    ADD CONSTRAINT ck_ex_origin CHECK (content_origin IN ('SELF_AUTHORED', 'ADAPTED')),
    ADD CONSTRAINT ck_ex_review CHECK (review_status IN ('DRAFT', 'PENDING', 'APPROVED', 'NEEDS_REVIEW')),
    ADD INDEX ix_ex_review (review_status);

CREATE TABLE exercise_sources (
    exercise_id BIGINT NOT NULL,
    source_id   BIGINT NOT NULL,
    PRIMARY KEY (exercise_id, source_id),
    CONSTRAINT fk_exsrc_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT fk_exsrc_source FOREIGN KEY (source_id) REFERENCES content_sources (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE articles (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slug           VARCHAR(120)  NOT NULL,
    title          VARCHAR(200)  NOT NULL,
    -- NUTRITION | HYDRATION | SLEEP | ACTIVITY | RECOVERY
    category       VARCHAR(20)   NOT NULL,
    summary        VARCHAR(500)  NOT NULL,
    body           TEXT          NOT NULL,
    content_origin VARCHAR(20)   NOT NULL DEFAULT 'SELF_AUTHORED',
    -- DRAFT -> PENDING -> PUBLISHED -> ARCHIVED; ADMIN trả về DRAFT kèm ghi chú nếu cần sửa
    status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    review_note    VARCHAR(500),
    author_id      BIGINT,
    reviewed_by    BIGINT,
    reviewed_at    DATETIME(6),
    published_at   DATETIME(6),
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6),
    CONSTRAINT uq_article_slug UNIQUE (slug),
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_article_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_article_slug CHECK (REGEXP_LIKE(slug, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT ck_article_category CHECK (category IN ('NUTRITION', 'HYDRATION', 'SLEEP', 'ACTIVITY', 'RECOVERY')),
    CONSTRAINT ck_article_origin CHECK (content_origin IN ('SELF_AUTHORED', 'ADAPTED')),
    CONSTRAINT ck_article_status CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'ARCHIVED')),
    INDEX ix_article_status (status, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE article_sources (
    article_id BIGINT NOT NULL,
    source_id  BIGINT NOT NULL,
    PRIMARY KEY (article_id, source_id),
    CONSTRAINT fk_artsrc_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_artsrc_source FOREIGN KEY (source_id) REFERENCES content_sources (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
