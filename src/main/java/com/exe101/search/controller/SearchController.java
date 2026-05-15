package com.exe101.search.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.search.dto.SearchHistoryRequest;
import com.exe101.search.dto.SearchHistoryResponse;
import com.exe101.search.dto.SearchInitialResponse;
import com.exe101.search.dto.SearchItemDTO;
import com.exe101.search.dto.SearchItemType;
import com.exe101.search.dto.SearchPageResponse;
import com.exe101.search.dto.SearchSortType;
import com.exe101.search.dto.SearchSuccessResponse;
import com.exe101.search.dto.SearchSuggestionsResponse;
import com.exe101.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/initial")
    public ResponseEntity<SearchInitialResponse> getInitial(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "10") int recommendedSize
    ) {
        Long userId = principal != null ? principal.getUser().getId() : null;
        return ResponseEntity.ok(searchService.getInitial(userId, lat, lng, recommendedSize));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<SearchSuggestionsResponse> getSuggestions(
            @RequestParam String keyword,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(searchService.getSuggestions(keyword, lat, lng, radiusKm, size));
    }

    @GetMapping
    public ResponseEntity<SearchPageResponse<SearchItemDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "ALL") SearchItemType type,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "RELEVANT") SearchSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(searchService.search(keyword, type, lat, lng, radiusKm, sort, page, size));
    }

    @GetMapping("/history")
    public ResponseEntity<SearchHistoryResponse> getHistory(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(searchService.getHistory(getCurrentUserId(principal)));
    }

    @PostMapping("/history")
    public ResponseEntity<SearchSuccessResponse> saveHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SearchHistoryRequest request
    ) {
        searchService.saveHistory(getCurrentUserId(principal), request.getKeyword());
        return ResponseEntity.ok(new SearchSuccessResponse(true));
    }

    @DeleteMapping("/history")
    public ResponseEntity<SearchSuccessResponse> deleteHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String keyword
    ) {
        searchService.deleteHistory(getCurrentUserId(principal), keyword);
        return ResponseEntity.ok(new SearchSuccessResponse(true));
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalArgumentException("Cần người dùng đã đăng nhập");
        }
        return principal.getUser().getId();
    }
}

