package com.gymflow.web;

import com.gymflow.domain.ArticleCategory;
import com.gymflow.domain.ArticleStatus;
import com.gymflow.dto.Dtos.*;
import com.gymflow.security.CurrentUser;
import com.gymflow.service.ArticleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Kho kiến thức")
@RestController
@RequiredArgsConstructor
public class KnowledgeController {

    private final ArticleService articleService;

    // ---------- Công khai: chỉ bài đã xuất bản ----------
    @GetMapping("/api/articles/public")
    public List<ArticleSummary> publicArticles(@RequestParam(required = false) ArticleCategory category,
                                               @RequestParam(defaultValue = "") String q) {
        return articleService.published(category, q);
    }

    @GetMapping("/api/articles/public/{slug}")
    public ArticleResponse publicArticle(@PathVariable String slug) {
        return articleService.publishedBySlug(slug);
    }

    // ---------- Nội bộ ----------
    @GetMapping("/api/articles")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public List<ArticleResponse> articles(@RequestParam(required = false) ArticleStatus status) {
        return articleService.all(status);
    }

    @GetMapping("/api/articles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ArticleResponse article(@PathVariable Long id) {
        return articleService.get(id);
    }

    @PostMapping("/api/articles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ArticleResponse createArticle(@Valid @RequestBody ArticleRequest req) {
        return articleService.create(CurrentUser.get(), req);
    }

    @PutMapping("/api/articles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ArticleResponse updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest req) {
        return articleService.update(CurrentUser.get(), id, req);
    }

    /** SUBMIT: tác giả hoặc ADMIN · APPROVE / REJECT / ARCHIVE / RESTORE: chỉ ADMIN (kiểm tra ở service). */
    @PutMapping("/api/articles/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ArticleResponse articleAction(@PathVariable Long id, @Valid @RequestBody ArticleActionRequest req) {
        return articleService.act(CurrentUser.get(), id, req);
    }

    @DeleteMapping("/api/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
    }
}
