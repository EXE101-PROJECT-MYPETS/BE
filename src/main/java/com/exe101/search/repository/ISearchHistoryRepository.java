package com.exe101.search.repository;

import com.exe101.search.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ISearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop20ByUserIdOrderByLastSearchedAtDescIdDesc(Long userId);

    Optional<SearchHistory> findByUserIdAndKeywordIgnoreCase(Long userId, String keyword);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndKeywordIgnoreCase(Long userId, String keyword);
}

