package com.exe101.ai.service;

import com.exe101.ai.dto.PetContext;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class QueryRewriteService {

    public String rewrite(String message, PetContext petContext) {
        if (message == null || message.isBlank()) {
            return "";
        }

        String q = message.toLowerCase(Locale.ROOT);

        q = q.replace("be nha toi", "thu cung");
        q = q.replace("bé nhà tôi", "thú cưng");
        q = q.replace("be", "thu cung");
        q = q.replace("bé", "thú cưng");
        q = q.replace("boss", "thú cưng");
        q = q.replace("oi", "non");
        q = q.replace("ói", "nôn");
        q = q.replace("bo bua", "bo an");
        q = q.replace("bỏ bữa", "bỏ ăn");
        q = q.replace("khong an", "bo an");
        q = q.replace("không ăn", "bỏ ăn");
        q = q.replace("an it", "bo an");
        q = q.replace("ăn ít", "bỏ ăn");
        q = q.replace("di ngoai", "tieu chay");
        q = q.replace("đi ngoài", "tiêu chảy");

        String species = resolveSpeciesKeyword(petContext);
        String suffix = " chăm sóc thú cưng dấu hiệu nguy hiểm thú y";

        if (!species.isBlank() && !q.contains(species)) {
            return species + " " + q + suffix;
        }

        return q + suffix;
    }

    private String resolveSpeciesKeyword(PetContext petContext) {
        if (petContext == null || petContext.getSpecies() == null) {
            return "";
        }
        return switch (petContext.getSpecies()) {
            case "DOG" -> "chó";
            case "CAT" -> "mèo";
            default -> "thú cưng";
        };
    }
}
