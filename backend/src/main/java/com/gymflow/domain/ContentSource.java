package com.gymflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Nguồn tham khảo kèm giấy phép. Bản ghi do migration tạo; ứng dụng chỉ đọc. */
@Entity
@Table(name = "content_sources")
@Getter
@Setter
@NoArgsConstructor
public class ContentSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    private String license;

    @Column(name = "license_url")
    private String licenseUrl;

    /** FALSE: nhóm chưa đối chiếu được điều khoản giấy phép trên trang gốc. */
    @Column(name = "license_verified", nullable = false)
    private boolean licenseVerified;

    private String attribution;

    @Column(name = "publisher_reviewed")
    private LocalDate publisherReviewed;

    @Column(name = "accessed_at", nullable = false)
    private LocalDate accessedAt;
}
