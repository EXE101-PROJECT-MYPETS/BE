package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeSearchResult;
import com.exe101.ai.entity.AiPetChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiPromptBuilder {

    public String buildPetHealthPrompt(
            String userMessage,
            String petContext,
            List<AiKnowledgeSearchResult> knowledgeResults,
            List<AiPetChatMessage> recentMessages
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ban la tro ly AI ho tro suc khoe thu cung.\n");
        builder.append("Quy tac bat buoc:\n");
        builder.append("- Khong phai bac si thu y va khong duoc khang dinh chan doan chac chan.\n");
        builder.append("- Chi dua ra tu van tham khao dua tren context duoc cung cap.\n");
        builder.append("- Neu co dau hieu nguy hiem thi phai khuyen den co so thu y ngay.\n");
        builder.append("- Tra loi bang tieng Viet.\n");
        builder.append("- Ket qua phai la JSON hop le voi cau truc:\n");
        builder.append("{\"answer\":\"...\",\"riskLevel\":\"LOW|MEDIUM|HIGH|EMERGENCY\",\"shouldBookVet\":true,\"recommendedActions\":[\"...\"]}\n\n");

        builder.append("Pet context:\n").append(petContext).append("\n");

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

        builder.append("Tai lieu tham khao:\n");
        if (knowledgeResults == null || knowledgeResults.isEmpty()) {
            builder.append("- Chua co tai lieu lien quan\n");
        } else {
            knowledgeResults.forEach(result -> builder
                    .append("- [")
                    .append(result.getSourceType())
                    .append("] ")
                    .append(result.getTitle() == null || result.getTitle().isBlank() ? "(khong co tieu de)" : result.getTitle())
                    .append(": ")
                    .append(result.getContent())
                    .append('\n'));
        }

        builder.append("Cau hoi cua nguoi dung:\n");
        builder.append(userMessage);
        return builder.toString();
    }
}
