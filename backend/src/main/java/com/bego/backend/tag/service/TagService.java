package com.bego.backend.tag.service;

import com.bego.backend.tag.dto.CreateTagRequest;
import com.bego.backend.tag.dto.ReorderTagsRequest;
import com.bego.backend.tag.dto.UpdateTagRequest;
import com.bego.backend.tag.vo.TagResponse;
import java.util.List;

public interface TagService {
    List<TagResponse> list(Long userId);

    TagResponse create(Long userId, CreateTagRequest request);

    TagResponse update(Long userId, Long tagId, UpdateTagRequest request);

    void delete(Long userId, Long tagId);

    void reorder(Long userId, ReorderTagsRequest request);
}
