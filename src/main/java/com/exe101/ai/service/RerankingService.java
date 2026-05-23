package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeSearchResult;
import com.exe101.ai.dto.PetContext;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RerankingService {

    public List<AiKnowledgeSearchResult> rerank(
            String originalMessage,
            String rewrittenQuery,
            PetContext petContext,
            List<AiKnowledgeSearchResult> candidates
    ) {
        return candidates.stream()
                .map(item -> {
                    item.setRerankScore(calculateScore(originalMessage, petContext, item));
                    return item;
                })
                .sorted(Comparator.comparing(AiKnowledgeSearchResult::getRerankScore).reversed())
                .toList();
    }

    private double calculateScore(
            String originalMessage,
            PetContext petContext,
            AiKnowledgeSearchResult item
    ) {
        double score = 0.0;

        score += 0.6 * item.getVectorScore();
        score += 0.3 * item.getKeywordScore();

        String msg = safeLower(originalMessage);
        String title = safeLower(item.getTitle());
        String content = safeLower(item.getContent());
        String topic = safeLower(item.getTopic());

        if (containsAny(msg, "bo an", "bỏ ăn", "non", "nôn", "oi", "ói", "tieu chay", "tiêu chảy",
                "co giat", "co giật", "kho tho", "khó thở", "chay mau", "chảy máu",
                "ngo doc", "ngộ độc", "lo do", "lờ đờ", "mat nuoc", "mất nước")) {
            if (topic.contains("health") || topic.contains("emergency")
                    || title.contains("nguy hiem") || title.contains("nguy hiểm")
                    || content.contains("nguy hiem") || content.contains("nguy hiểm")) {
                score += 0.15;
            }
        }

        if (petContext != null && petContext.getSpecies() != null) {
            String speciesKeyword = switch (petContext.getSpecies()) {
                case "DOG" -> "chó";
                case "CAT" -> "mèo";
                default -> "";
            };

            if (!speciesKeyword.isBlank() && (title.contains(speciesKeyword) || content.contains(speciesKeyword)
                    || title.contains(removeVietnameseTone(speciesKeyword)) || content.contains(removeVietnameseTone(speciesKeyword)))) {
                score += 0.08;
            }
        }

        if (containsAny(msg, "dat lich", "đặt lịch", "booking", "spa", "grooming", "tam", "tắm",
                "cat mong", "cắt móng", "dich vu", "dịch vụ")) {
            if (topic.contains("service") || topic.contains("booking") || topic.contains("grooming")) {
                score += 0.1;
            }
        }

        return score;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String removeVietnameseTone(String value) {
        return value
                .replace("ó", "o")
                .replace("ò", "o")
                .replace("ỏ", "o")
                .replace("õ", "o")
                .replace("ọ", "o")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ẻ", "e")
                .replace("ẽ", "e")
                .replace("ẹ", "e")
                .replace("ê", "e")
                .replace("ề", "e")
                .replace("ế", "e")
                .replace("ể", "e")
                .replace("ễ", "e")
                .replace("ệ", "e");
    }
}
