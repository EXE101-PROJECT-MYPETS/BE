package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeSearchResult;
import com.exe101.ai.dto.PetContext;
import com.exe101.ai.entity.AiPetChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiPromptBuilder {

    public String buildPetHealthSystemInstruction() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ban la tro ly AI cua ung dung PetPee.\n\n");
        builder.append("Nhiem vu cua ban:\n");
        builder.append("- Ho tro nguoi dung ve cham soc thu cung, suc khoe co ban, hanh vi, dinh duong, ve sinh, grooming, san pham, dich vu, shop va booking tren PetPee.\n");
        builder.append("- Tra loi bang tieng Viet, than thien, de hieu, ngan gon nhung du y.\n");
        builder.append("- Uu tien tra loi dua tren context duoc cung cap tu he thong.\n");
        builder.append("- Neu context khong du, hay noi ro la chua du thong tin va dua ra huong xu ly an toan.\n\n");
        builder.append("Gioi han:\n");
        builder.append("- Ban khong phai bac si thu y.\n");
        builder.append("- Khong chan doan chac chan benh.\n");
        builder.append("- Khong ke don thuoc.\n");
        builder.append("- Khong dua lieu luong thuoc cu the.\n");
        builder.append("- Neu thu cung co dau hieu nguy hiem nhu bo an lau, non nhieu, tieu chay nang, kho tho, co giat, chay mau, lo do, mat nuoc, dau du doi hoac nghi ngo doc, hay khuyen nguoi dung lien he bac si thu y/co so thu y ngay.\n\n");
        builder.append("Pham vi:\n");
        builder.append("- Chi tra loi cac cau hoi lien quan den thu cung hoac dich vu/san pham trong he sinh thai PetPee.\n");
        builder.append("- Neu cau hoi khong lien quan, hay tu choi lich su va huong nguoi dung quay lai chu de thu cung.\n\n");
        builder.append("Yeu cau dinh dang:\n");
        builder.append("- Ket qua phai la JSON hop le voi cau truc:\n");
        builder.append("{\"answer\":\"...\",\"riskLevel\":\"LOW|MEDIUM|HIGH|EMERGENCY\",\"shouldBookVet\":true,\"recommendedActions\":[\"...\"]}\n");
        builder.append("- Truong answer phai la noi dung tieng Viet tu nhien gui cho nguoi dung.\n");
        builder.append("- riskLevel phan anh muc do rui ro cua tinh huong.\n");
        builder.append("- shouldBookVet = true neu nen dua thu cung di co so thu y.\n");
        builder.append("- recommendedActions la danh sach hanh dong an toan, ngan gon, thuc te.\n");
        return builder.toString();
    }

    public String buildPetHealthPrompt(
            PetContext petContext,
            List<AiKnowledgeSearchResult> knowledgeResults,
            List<AiPetChatMessage> recentMessages,
            String userMessage
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("Thong tin thu cung:\n");
        builder.append("- Ten: ").append(safe(petContext == null ? null : petContext.getName())).append('\n');
        builder.append("- Loai: ").append(safe(petContext == null ? null : petContext.getSpeciesName())).append('\n');
        builder.append("- Giong: ").append(safe(petContext == null ? null : petContext.getBreed())).append('\n');
        builder.append("- Tuoi: ").append(safe(petContext == null ? null : petContext.getAge())).append('\n');
        builder.append("- Can nang: ").append(safe(petContext == null ? null : petContext.getWeight())).append('\n');
        builder.append("- Ghi chu suc khoe: ")
                .append("di ung: ").append(safe(petContext == null ? null : petContext.getAllergies()))
                .append("; benh nen: ").append(safe(petContext == null ? null : petContext.getConditions()))
                .append("; ghi chu: ").append(safe(petContext == null ? null : petContext.getHealthNotes()))
                .append("\n\n");

        builder.append("Lich su hoi dap gan day:\n");
        if (recentMessages == null || recentMessages.isEmpty()) {
            builder.append("- Chua co lich su\n");
        } else {
            recentMessages.forEach(message -> builder
                    .append("- ")
                    .append(message.getRole().name())
                    .append(": ")
                    .append(message.getContent())
                    .append('\n'));
        }
        builder.append('\n');

        builder.append("Context tu he thong:\n");
        if (knowledgeResults == null || knowledgeResults.isEmpty()) {
            builder.append("- Chua co context phu hop\n");
        } else {
            for (int i = 0; i < Math.min(5, knowledgeResults.size()); i++) {
                AiKnowledgeSearchResult result = knowledgeResults.get(i);
                builder.append('[').append(i + 1).append("] ")
                        .append(safe(result.getTitle()))
                        .append('\n')
                        .append(safe(result.getContent()))
                        .append("\n\n");
            }
        }

        builder.append("Cau hoi nguoi dung:\n");
        builder.append(userMessage == null ? "" : userMessage);
        return builder.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "khong co" : value;
    }
}
