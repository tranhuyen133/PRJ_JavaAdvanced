package com.gymflow.service;

import com.gymflow.common.ApiException;
import com.gymflow.domain.*;
import com.gymflow.dto.Dtos.*;
import com.gymflow.repository.ArticleRepository;
import com.gymflow.repository.ContentSourceRepository;
import com.gymflow.repository.UserRepository;
import com.gymflow.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Kho kiến thức. Luồng: DRAFT → (SUBMIT) PENDING → (APPROVE, chỉ ADMIN) PUBLISHED → (ARCHIVE) ARCHIVED.
 * Trang công khai chỉ đọc bài PUBLISHED. HLV soạn và gửi duyệt bài của chính mình, không tự xuất bản được.
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ContentSourceRepository sourceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Mappers mappers;

    // ---------- Công khai ----------
    @Transactional(readOnly = true)
    public List<ArticleSummary> published(ArticleCategory category, String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED).stream()
                .filter(a -> category == null || a.getCategory() == category)
                .filter(a -> needle.isEmpty() || a.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                        || a.getSummary().toLowerCase(Locale.ROOT).contains(needle))
                .map(mappers::articleSummary).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse publishedBySlug(String slug) {
        return articleRepository.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED)
                .map(a -> mappers.article(a, false))
                .orElseThrow(() -> ApiException.notFound("bài viết"));
    }

    // ---------- Nội bộ (ADMIN, TRAINER) ----------
    @Transactional(readOnly = true)
    public List<ArticleResponse> all(ArticleStatus status) {
        return articleRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc().stream()
                .filter(a -> status == null || a.getStatus() == status)
                .map(a -> mappers.article(a, true)).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse get(Long id) {
        return mappers.article(load(id), true);
    }

    @Transactional
    public ArticleResponse create(AuthUser user, ArticleRequest req) {
        if (articleRepository.existsBySlug(req.slug())) throw ApiException.conflict("Đường dẫn bài viết đã tồn tại");
        Article a = new Article();
        apply(a, req);
        a.setStatus(ArticleStatus.DRAFT);
        a.setAuthor(userRepository.getReferenceById(user.id()));
        articleRepository.save(a);
        auditService.log("ARTICLE_CREATE", "ARTICLE", a.getId(), a.getTitle());
        return mappers.article(a, true);
    }

    /** Sửa bài đã xuất bản (không phải ADMIN) sẽ đưa bài về "Chờ duyệt" và gỡ khỏi trang công khai. */
    @Transactional
    public ArticleResponse update(AuthUser user, Long id, ArticleRequest req) {
        Article a = load(id);
        ensureCanEdit(user, a);
        if (articleRepository.existsBySlugAndIdNot(req.slug(), id)) throw ApiException.conflict("Đường dẫn bài viết đã tồn tại");
        apply(a, req);
        if (!user.isAdmin() && a.getStatus() == ArticleStatus.PUBLISHED) {
            a.setStatus(ArticleStatus.PENDING);
            a.setPublishedAt(null);
        }
        auditService.log("ARTICLE_UPDATE", "ARTICLE", a.getId(), a.getTitle());
        return mappers.article(a, true);
    }

    @Transactional
    public ArticleResponse act(AuthUser user, Long id, ArticleActionRequest req) {
        Article a = load(id);
        String note = Mappers.blankToNull(req.note());
        switch (req.action()) {
            case SUBMIT -> {
                ensureCanEdit(user, a);
                require(a, ArticleStatus.DRAFT);
                a.setStatus(ArticleStatus.PENDING);
            }
            case APPROVE -> {
                ensureAdmin(user);
                require(a, ArticleStatus.PENDING);
                if (a.getSources().isEmpty()) throw ApiException.badRequest("Bài viết cần ít nhất một nguồn tham khảo trước khi xuất bản");
                a.setStatus(ArticleStatus.PUBLISHED);
                a.setPublishedAt(LocalDateTime.now());
                markReviewed(user, a, note);
            }
            case REJECT -> {
                ensureAdmin(user);
                require(a, ArticleStatus.PENDING);
                if (note == null) throw ApiException.badRequest("Vui lòng ghi rõ nội dung cần chỉnh sửa");
                a.setStatus(ArticleStatus.DRAFT);
                markReviewed(user, a, note);
            }
            case ARCHIVE -> {
                ensureAdmin(user);
                require(a, ArticleStatus.PUBLISHED);
                a.setStatus(ArticleStatus.ARCHIVED);
            }
            case RESTORE -> {
                ensureAdmin(user);
                require(a, ArticleStatus.ARCHIVED);
                a.setStatus(ArticleStatus.DRAFT);
                a.setPublishedAt(null);
            }
        }
        auditService.log("ARTICLE_" + req.action().name(), "ARTICLE", a.getId(), a.getTitle());
        return mappers.article(a, true);
    }

    /** Bài đang xuất bản phải gỡ (ARCHIVE) trước khi xoá. */
    @Transactional
    public void delete(Long id) {
        Article a = load(id);
        if (a.getStatus() == ArticleStatus.PUBLISHED) throw ApiException.conflict("Hãy gỡ bài viết trước khi xoá");
        articleRepository.delete(a);
        auditService.log("ARTICLE_DELETE", "ARTICLE", id, a.getTitle());
    }

    private void apply(Article a, ArticleRequest req) {
        a.setTitle(req.title().trim());
        a.setSlug(req.slug());
        a.setCategory(req.category());
        a.setSummary(req.summary().trim());
        a.setBody(req.body().trim());
        if (req.contentOrigin() != null) a.setContentOrigin(req.contentOrigin());
        if (req.sourceIds() != null) a.setSources(ExerciseService.resolveSources(sourceRepository, req.sourceIds()));
    }

    private void markReviewed(AuthUser user, Article a, String note) {
        a.setReviewNote(note);
        a.setReviewedBy(userRepository.getReferenceById(user.id()));
        a.setReviewedAt(LocalDateTime.now());
    }

    private static void require(Article a, ArticleStatus expected) {
        if (a.getStatus() != expected) throw ApiException.conflict("Thao tác không hợp lệ với trạng thái hiện tại của bài viết");
    }

    private static void ensureAdmin(AuthUser user) {
        if (!user.isAdmin()) throw ApiException.forbidden();
    }

    /** ADMIN sửa mọi bài; HLV chỉ sửa bài do mình soạn. */
    private static void ensureCanEdit(AuthUser user, Article a) {
        if (user.isAdmin()) return;
        if (a.getAuthor() == null || !a.getAuthor().getId().equals(user.id())) throw ApiException.forbidden();
    }

    private Article load(Long id) {
        return articleRepository.findById(id).orElseThrow(() -> ApiException.notFound("bài viết"));
    }
}
