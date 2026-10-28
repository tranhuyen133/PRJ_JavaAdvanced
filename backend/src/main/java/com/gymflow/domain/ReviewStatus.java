package com.gymflow.domain;

/** Trạng thái duyệt nội dung bài tập. Chỉ ADMIN chuyển sang APPROVED. */
public enum ReviewStatus {
    DRAFT, PENDING, APPROVED, NEEDS_REVIEW
}
