package com.exe101.ai.controller;

import com.exe101.ai.dto.AiKnowledgeCreateRequest;
import com.exe101.ai.dto.AiKnowledgeCreateResponse;
import com.exe101.ai.dto.AiKnowledgeSearchResult;
import com.exe101.ai.dto.AiKnowledgeSearchTestRequest;
import com.exe101.ai.service.AiKnowledgeIngestionService;
import com.exe101.ai.service.AiKnowledgeSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeAdminController {

    private final AiKnowledgeIngestionService aiKnowledgeIngestionService;
    private final AiKnowledgeSearchService aiKnowledgeSearchService;

    @PostMapping
    public ResponseEntity<AiKnowledgeCreateResponse> createKnowledge(
            @Valid @RequestBody AiKnowledgeCreateRequest request
    ) {
        return ResponseEntity.ok(aiKnowledgeIngestionService.createKnowledge(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AiKnowledgeCreateResponse>> createKnowledgeBulk(
            @Valid @RequestBody List<AiKnowledgeCreateRequest> requests
    ) {
        return ResponseEntity.ok(aiKnowledgeIngestionService.createKnowledgeBulk(requests));
    }

    @PostMapping("/search-test")
    public ResponseEntity<List<AiKnowledgeSearchResult>> searchTest(
            @Valid @RequestBody AiKnowledgeSearchTestRequest request
    ) {
        return ResponseEntity.ok(aiKnowledgeSearchService.search(
                request.getQuery(),
                request.getLimit() == null ? 5 : request.getLimit()
        ));
    }
}
