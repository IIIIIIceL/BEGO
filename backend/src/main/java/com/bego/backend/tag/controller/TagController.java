package com.bego.backend.tag.controller;

import com.bego.backend.common.security.CurrentUserContext;
import com.bego.backend.tag.dto.CreateTagRequest;
import com.bego.backend.tag.dto.ReorderTagsRequest;
import com.bego.backend.tag.dto.UpdateTagRequest;
import com.bego.backend.tag.service.TagService;
import com.bego.backend.tag.vo.TagResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> list() {
        return tagService.list(CurrentUserContext.requireUserId());
    }

    @PostMapping
    public TagResponse create(@Valid @RequestBody CreateTagRequest request) {
        return tagService.create(CurrentUserContext.requireUserId(), request);
    }

    @PutMapping("/{id}")
    public TagResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTagRequest request
    ) {
        return tagService.update(CurrentUserContext.requireUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(CurrentUserContext.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@Valid @RequestBody ReorderTagsRequest request) {
        tagService.reorder(CurrentUserContext.requireUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
