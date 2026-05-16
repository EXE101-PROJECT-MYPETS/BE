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

        builder.append("Bạn là trợ lý AI của ứng dụng PetPee.\n\n");

        builder.append("Nhiệm vụ của bạn:\n");
        builder.append("- Hỗ trợ người dùng về chăm sóc thú cưng, sức khỏe cơ bản, hành vi, dinh dưỡng, vệ sinh, grooming, sản phẩm, dịch vụ, shop và booking trên PetPee.\n");
        builder.append("- Trả lời bằng tiếng Việt, thân thiện, dễ hiểu, ngắn gọn nhưng đủ ý.\n");
        builder.append("- Ưu tiên trả lời dựa trên context được cung cấp từ hệ thống.\n");
        builder.append("- Nếu context không đủ, hãy nói rõ là chưa đủ thông tin và đưa ra hướng xử lý an toàn.\n\n");

        builder.append("Giới hạn:\n");
        builder.append("- Bạn không phải bác sĩ thú y.\n");
        builder.append("- Không chẩn đoán chắc chắn bệnh.\n");
        builder.append("- Không kê đơn thuốc.\n");
        builder.append("- Không đưa liều lượng thuốc cụ thể.\n");
        builder.append("- Nếu thú cưng có dấu hiệu nguy hiểm như bỏ ăn lâu, nôn nhiều, tiêu chảy nặng, khó thở, co giật, chảy máu, lờ đờ, mất nước, đau dữ dội hoặc nghi ngộ độc, hãy khuyên người dùng liên hệ bác sĩ thú y/cơ sở thú y ngay.\n\n");

        builder.append("Phạm vi:\n");
        builder.append("- Chỉ trả lời các câu hỏi liên quan đến thú cưng hoặc dịch vụ/sản phẩm trong hệ sinh thái PetPee.\n");
        builder.append("- Nếu câu hỏi không liên quan, hãy từ chối lịch sự và hướng người dùng quay lại chủ đề thú cưng.\n\n");

        builder.append("Yêu cầu định dạng:\n");
        builder.append("- Kết quả phải là JSON hợp lệ với cấu trúc:\n");
        builder.append("- Neu nguoi dung muon dat lich, grooming, tam, spa, cat mong, kham thu y hoac tim shop/dich vu, action.type phai la OPEN_BOOKING_FLOW.\n");
        builder.append("- Khong duoc noi da dat lich thanh cong neu he thong chua tao booking that.\n");
        builder.append("- MVP khong tu tao booking trong chat, chi dieu huong nguoi dung sang man dat lich de chon shop, dich vu, thoi gian va xac nhan.\n");
        builder.append("{\"answer\":\"...\",\"riskLevel\":\"LOW|MEDIUM|HIGH|EMERGENCY\",\"shouldBookVet\":true,\"recommendedActions\":[\"...\"],\"action\":{\"type\":\"NONE|OPEN_BOOKING_FLOW\",\"toolName\":\"...\",\"arguments\":{},\"missingFields\":[]}}\n");
        builder.append("- Trường answer phải là nội dung tiếng Việt tự nhiên gửi cho người dùng.\n");
        builder.append("- riskLevel phản ánh mức độ rủi ro của tình huống.\n");
        builder.append("- shouldBookVet = true nếu nên đưa thú cưng đi cơ sở thú y.\n");
        builder.append("- recommendedActions là danh sách hành động an toàn, ngắn gọn, thực tế.\n");

        return builder.toString();
    }

    public String buildPetHealthPrompt(
            PetContext petContext,
            List<AiKnowledgeSearchResult> knowledgeResults,
            List<AiPetChatMessage> recentMessages,
            String userMessage
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("Thông tin thú cưng:\n");
        builder.append("- Tên: ").append(safe(petContext == null ? null : petContext.getName())).append('\n');
        builder.append("- Loài: ").append(safe(petContext == null ? null : petContext.getSpeciesName())).append('\n');
        builder.append("- Giống: ").append(safe(petContext == null ? null : petContext.getBreed())).append('\n');
        builder.append("- Tuổi: ").append(safe(petContext == null ? null : petContext.getAge())).append('\n');
        builder.append("- Cân nặng: ").append(safe(petContext == null ? null : petContext.getWeight())).append('\n');

        builder.append("- Ghi chú sức khỏe: ")
                .append("dị ứng: ").append(safe(petContext == null ? null : petContext.getAllergies()))
                .append("; bệnh nền: ").append(safe(petContext == null ? null : petContext.getConditions()))
                .append("; ghi chú: ").append(safe(petContext == null ? null : petContext.getHealthNotes()))
                .append('\n');

        builder.append("- Lịch sử tiêm phòng gần đây:\n")
                .append(safeMultiline(petContext == null ? null : petContext.getVaccinationSummary()))
                .append("\n\n");

        builder.append("Lịch sử hỏi đáp gần đây:\n");
        if (recentMessages == null || recentMessages.isEmpty()) {
            builder.append("- Chưa có lịch sử\n");
        } else {
            recentMessages.forEach(message -> builder
                    .append("- ")
                    .append(message.getRole().name())
                    .append(": ")
                    .append(message.getContent())
                    .append('\n'));
        }
        builder.append('\n');

        builder.append("Context từ hệ thống:\n");
        if (knowledgeResults == null || knowledgeResults.isEmpty()) {
            builder.append("- Chưa có context phù hợp\n");
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

        builder.append("Câu hỏi người dùng:\n");
        builder.append(userMessage == null ? "" : userMessage);

        return builder.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "không có" : value;
    }

    private String safeMultiline(String value) {
        return value == null || value.isBlank() ? "- không có" : value;
    }
}
