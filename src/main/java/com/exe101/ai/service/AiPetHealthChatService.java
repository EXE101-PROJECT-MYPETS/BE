package com.exe101.ai.service;

import com.exe101.ai.dto.*;
import com.exe101.ai.entity.AiPetChatConversation;
import com.exe101.ai.entity.AiPetChatMessage;
import com.exe101.ai.enums.AiChatRole;
import com.exe101.ai.exception.AiAccessDenied;
import com.exe101.ai.exception.AiNotFound;
import com.exe101.ai.repository.AiPetChatConversationRepository;
import com.exe101.ai.repository.AiPetChatMessageRepository;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.pet.entity.Pet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPetHealthChatService {

    private static final String AI_BUSY_FALLBACK_MESSAGE = "AI đang bận, vui lòng chờ khoảng 5 phút rồi hỏi lại.";

    private final AiPetChatConversationRepository conversationRepository;
    private final AiPetChatMessageRepository messageRepository;
    private final AiPetContextService aiPetContextService;
    private final AiKnowledgeSearchService aiKnowledgeSearchService;
    private final QueryRewriteService queryRewriteService;
    private final AiEmbeddingService aiEmbeddingService;
    private final RerankingService rerankingService;
    private final AiPromptBuilder aiPromptBuilder;
    private final GeminiClientService geminiClientService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiPetChatResponse chat(AiPetChatRequest request) {
        return chatAndCollect(request).response();
    }

    @Transactional
    public AiPetChatExecution chatAndCollect(AiPetChatRequest request) {
        Long currentUserId = getCurrentUserId();
        Pet pet = aiPetContextService.getAuthorizedPet(request.getPetId(), currentUserId);
        AiPetChatConversation conversation = resolveConversation(request, pet, currentUserId);

        AiPetChatMessage userMessage = new AiPetChatMessage();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(AiChatRole.USER);
        userMessage.setContent(request.getMessage().trim());
        userMessage.setMetadata(objectMapper.createObjectNode());
        messageRepository.save(userMessage);

        List<AiPetChatMessage> recentMessages = new ArrayList<>(messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversation.getId()));
        recentMessages.sort(Comparator.comparing(AiPetChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        try {
            PetContext petContext = aiPetContextService.loadPetContext(pet.getId(), currentUserId);
            String rewrittenQuery = queryRewriteService.rewrite(request.getMessage(), petContext);
            List<Double> embedding = aiEmbeddingService.embed(rewrittenQuery);
            String embeddingText = aiEmbeddingService.toPgVectorString(embedding);
            List<AiKnowledgeSearchResult> candidates = aiKnowledgeSearchService.hybridSearch(
                    rewrittenQuery,
                    embeddingText,
                    30,
                    20
            );
            List<AiKnowledgeSearchResult> finalContexts = rerankingService.rerank(
                            request.getMessage(),
                            rewrittenQuery,
                            petContext,
                            candidates
                    )
                    .stream()
                    .limit(5)
                    .toList();

            log.info("AI original message: {}", request.getMessage());
            log.info("AI rewritten query: {}", rewrittenQuery);
            for (AiKnowledgeSearchResult item : finalContexts) {
                log.info(
                        "AI selected context: id={}, title={}, vectorScore={}, keywordScore={}, hybridScore={}, rerankScore={}",
                        item.getId(),
                        item.getTitle(),
                        item.getVectorScore(),
                        item.getKeywordScore(),
                        item.getHybridScore(),
                        item.getRerankScore()
                );
            }

            String systemInstruction = aiPromptBuilder.buildPetHealthSystemInstruction();
            String prompt = aiPromptBuilder.buildPetHealthPrompt(petContext, finalContexts, recentMessages, request.getMessage());
            String rawAnswer = geminiClientService.generateText(systemInstruction, prompt);
            if (rawAnswer == null) {
                rawAnswer = "";
            }
            AiResponsePayload payload = parseAiResponse(rawAnswer, request);
            return buildExecution(conversation.getId(), currentUserId, userMessage, createAssistantMessage(conversation.getId(), payload), payload);
        } catch (Exception ex) {
            log.error("AI pet health chat failed for conversationId={}, petId={}", conversation.getId(), pet.getId(), ex);
            AiResponsePayload fallbackPayload = fallbackPayload(request);
            return buildExecution(conversation.getId(), currentUserId, userMessage, createAssistantMessage(conversation.getId(), fallbackPayload), fallbackPayload);
        }
    }

    @Transactional(readOnly = true)
    public List<AiPetChatConversationDTO> getConversations(Long petId) {
        Long currentUserId = getCurrentUserId();
        Pet pet = aiPetContextService.getAuthorizedPet(petId, currentUserId);
        return conversationRepository.findByPetIdOrderByUpdatedAtDesc(pet.getId()).stream()
                .map(this::toConversationDTO)
                .toList();
    }

    @Transactional
    public AiPetChatConversationDTO getOrCreateConversation(Long petId) {
        Long currentUserId = getCurrentUserId();
        Pet pet = aiPetContextService.getAuthorizedPet(petId, currentUserId);
        return toConversationDTO(getOrCreateConversationForPet(pet, currentUserId, pet.getName()));
    }

    @Transactional(readOnly = true)
    public List<AiPetChatMessageDTO> getMessages(Long conversationId) {
        Long currentUserId = getCurrentUserId();
        AiPetChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AiNotFound("AiConversationNotFound", "Không tìm thấy cuộc trò chuyện AI"));
        validateConversationOwnership(conversation, currentUserId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageDTO)
                .toList();
    }

    private AiPetChatConversation resolveConversation(AiPetChatRequest request, Pet pet, Long currentUserId) {
        if (request.getConversationId() == null) {
            return getOrCreateConversationForPet(pet, currentUserId, request.getMessage());
        }

        AiPetChatConversation conversation = conversationRepository.findByIdAndPetId(request.getConversationId(), pet.getId())
                .orElseThrow(() -> new AiNotFound("AiConversationNotFound", "Không tìm thấy cuộc trò chuyện AI"));
        validateConversationOwnership(conversation, currentUserId);
        return conversation;
    }

    private AiPetChatConversation getOrCreateConversationForPet(Pet pet, Long currentUserId, String titleSource) {
        return conversationRepository.findFirstByPetIdOrderByUpdatedAtDesc(pet.getId())
                .map(conversation -> {
                    validateConversationOwnership(conversation, currentUserId);
                    return conversation;
                })
                .orElseGet(() -> {
                    AiPetChatConversation conversation = new AiPetChatConversation();
                    conversation.setPetId(pet.getId());
                    conversation.setUserId(currentUserId);
                    conversation.setTitle(buildTitle(titleSource));
                    return conversationRepository.save(conversation);
                });
    }

    private void validateConversationOwnership(AiPetChatConversation conversation, Long currentUserId) {
        aiPetContextService.getAuthorizedPet(conversation.getPetId(), currentUserId);
        if (conversation.getUserId() != null && !conversation.getUserId().equals(currentUserId)) {
            throw new AiAccessDenied("AiConversationAccessDenied", "Bạn không có quyền truy cập cuộc trò chuyện này");
        }
    }

    private String buildTitle(String message) {
        String normalized = message == null ? "" : message.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80);
    }

    private ObjectNode buildAssistantMetadata(AiResponsePayload payload) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("riskLevel", payload.riskLevel());
        metadata.put("shouldBookVet", payload.shouldBookVet());
        ArrayNode actions = metadata.putArray("recommendedActions");
        payload.recommendedActions().forEach(actions::add);
        metadata.set("action", objectMapper.valueToTree(payload.action()));
        return metadata;
    }

    private AiPetChatExecution buildExecution(
            Long conversationId,
            Long currentUserId,
            AiPetChatMessage userMessage,
            AiPetChatMessage assistantMessage,
            AiResponsePayload payload
    ) {
        conversationRepository.touch(conversationId);

        AiPetChatResponse response = new AiPetChatResponse(
                conversationId,
                payload.answer(),
                payload.riskLevel(),
                payload.shouldBookVet(),
                payload.recommendedActions(),
                payload.action()
        );
        AiPetChatMessageDTO userMessageDTO = toMessageDTO(userMessage);
        AiPetChatMessageDTO assistantMessageDTO = toMessageDTO(assistantMessage);
        return new AiPetChatExecution(
                currentUserId,
                response,
                new AiPetChatSocketEventDTO("USER_MESSAGE", conversationId, currentUserId, userMessageDTO, null),
                new AiPetChatSocketEventDTO("ASSISTANT_MESSAGE", conversationId, currentUserId, assistantMessageDTO, null),
                new AiPetChatSocketEventDTO("CHAT_RESPONSE", conversationId, currentUserId, null, response)
        );
    }

    private AiPetChatMessage createAssistantMessage(Long conversationId, AiResponsePayload payload) {
        AiPetChatMessage assistantMessage = new AiPetChatMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(AiChatRole.ASSISTANT);
        assistantMessage.setContent(payload.answer());
        assistantMessage.setMetadata(buildAssistantMetadata(payload));
        return messageRepository.save(assistantMessage);
    }

    private AiResponsePayload fallbackPayload(AiPetChatRequest request) {
        return new AiResponsePayload(
                AI_BUSY_FALLBACK_MESSAGE,
                "MEDIUM",
                false,
                List.of(),
                normalizeAction(defaultNoneAction(), request)
        );
    }

    private AiResponsePayload parseAiResponse(String rawAnswer, AiPetChatRequest request) {
        try {
            String normalized = stripMarkdownCodeFence(rawAnswer);
            JsonNode json = objectMapper.readTree(normalized);
            List<String> actions = new ArrayList<>();
            JsonNode recommendedActions = json.path("recommendedActions");
            if (recommendedActions.isArray()) {
                for (JsonNode action : recommendedActions) {
                    actions.add(action.asText());
                }
            }
            return new AiResponsePayload(
                    json.path("answer").asText(rawAnswer),
                    json.path("riskLevel").asText("MEDIUM"),
                    json.path("shouldBookVet").asBoolean(false),
                    actions,
                    normalizeAction(parseAction(json.path("action")), request)
            );
        } catch (Exception ex) {
            return new AiResponsePayload(rawAnswer, "MEDIUM", false, List.of(), normalizeAction(defaultNoneAction(), request));
        }
    }

    private AiAction parseAction(JsonNode actionNode) {
        if (actionNode == null || actionNode.isMissingNode() || !actionNode.isObject()) {
            return defaultNoneAction();
        }

        Map<String, Object> arguments = new LinkedHashMap<>();
        JsonNode argumentsNode = actionNode.path("arguments");
        if (argumentsNode.isObject()) {
            arguments.putAll(objectMapper.convertValue(argumentsNode, new TypeReference<Map<String, Object>>() {
            }));
        }

        List<String> missingFields = new ArrayList<>();
        JsonNode missingFieldsNode = actionNode.path("missingFields");
        if (missingFieldsNode.isArray()) {
            for (JsonNode field : missingFieldsNode) {
                missingFields.add(field.asText());
            }
        }

        return AiAction.builder()
                .type(actionNode.path("type").asText("NONE"))
                .toolName(actionNode.path("toolName").isNull() ? null : actionNode.path("toolName").asText(null))
                .arguments(arguments)
                .missingFields(missingFields)
                .build();
    }

    private AiAction normalizeAction(AiAction action, AiPetChatRequest request) {
        AiAction safeAction = action == null ? defaultNoneAction() : action;
        String type = safeAction.getType() == null ? "NONE" : safeAction.getType().trim();
        if (!"OPEN_BOOKING_FLOW".equals(type) && looksLikeBookingIntent(request == null ? null : request.getMessage())) {
            type = "OPEN_BOOKING_FLOW";
        }

        if (!"OPEN_BOOKING_FLOW".equals(type)) {
            return defaultNoneAction();
        }

        Map<String, Object> arguments = safeAction.getArguments() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(safeAction.getArguments());
        if (request != null && request.getPetId() != null) {
            arguments.put("petId", request.getPetId());
        }

        String message = request == null ? "" : request.getMessage();
        arguments.putIfAbsent("keyword", inferKeyword(message));
        arguments.putIfAbsent("serviceType", inferServiceType(message, String.valueOf(arguments.get("keyword"))));
        String preferredDateText = inferPreferredDateText(message);
        if (preferredDateText != null && !arguments.containsKey("preferredDateText")) {
            arguments.put("preferredDateText", preferredDateText);
        }

        return AiAction.builder()
                .type("OPEN_BOOKING_FLOW")
                .toolName(isBlank(safeAction.getToolName()) ? "open_booking_flow" : safeAction.getToolName())
                .arguments(arguments)
                .missingFields(safeAction.getMissingFields() == null ? List.of() : safeAction.getMissingFields())
                .build();
    }

    private AiAction defaultNoneAction() {
        return AiAction.builder()
                .type("NONE")
                .toolName(null)
                .arguments(Map.of())
                .missingFields(List.of())
                .build();
    }

    private boolean looksLikeBookingIntent(String message) {
        String text = safeLower(message);
        return containsAny(text,
                "dat lich", "đặt lịch", "booking", "grooming", "spa",
                "tam", "tắm", "cat mong", "cắt móng",
                "kham", "khám", "bac si", "bác sĩ", "thu y", "thú y",
                "dich vu", "dịch vụ", "shop");
    }

    private String inferKeyword(String message) {
        String text = safeLower(message);
        if (containsAny(text, "cat mong", "cắt móng")) {
            return "cắt móng";
        }
        if (containsAny(text, "tam", "tắm")) {
            return "tắm";
        }
        if (text.contains("spa")) {
            return "spa";
        }
        if (text.contains("grooming")) {
            return "grooming";
        }
        if (containsAny(text, "kham", "khám", "bac si", "bác sĩ", "thu y", "thú y")) {
            return "khám thú y";
        }
        if (containsAny(text, "dich vu", "dịch vụ")) {
            return "dịch vụ";
        }
        return "";
    }

    private String inferServiceType(String message, String keyword) {
        String text = safeLower(message) + " " + safeLower(keyword);
        if (containsAny(text, "tam", "tắm", "grooming", "spa", "cat mong", "cắt móng")) {
            return "GROOMING";
        }
        if (containsAny(text, "kham", "khám", "bac si", "bác sĩ", "thu y", "thú y")) {
            return "VET";
        }
        return "";
    }

    private String inferPreferredDateText(String message) {
        String text = safeLower(message);
        if (containsAny(text, "chieu mai", "chiều mai")) {
            return "chiều mai";
        }
        if (containsAny(text, "sang mai", "sáng mai")) {
            return "sáng mai";
        }
        if (containsAny(text, "toi mai", "tối mai")) {
            return "tối mai";
        }
        if (containsAny(text, "ngay mai", "ngày mai", "mai")) {
            return "ngày mai";
        }
        if (containsAny(text, "hom nay", "hôm nay")) {
            return "hôm nay";
        }
        return null;
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
        return value == null ? "" : value.toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stripMarkdownCodeFence(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return normalized;
    }

    private AiPetChatConversationDTO toConversationDTO(AiPetChatConversation conversation) {
        return new AiPetChatConversationDTO(
                conversation.getId(),
                conversation.getPetId(),
                conversation.getPet() == null ? null : conversation.getPet().getName(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private AiPetChatMessageDTO toMessageDTO(AiPetChatMessage message) {
        return new AiPetChatMessageDTO(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getMetadata(),
                message.getCreatedAt()
        );
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new AiAccessDenied("AiAuthenticationRequired", "Cần đăng nhập để sử dụng AI Pet Health");
        }
        return userPrincipal.getUser().getId();
    }

    private record AiResponsePayload(
            String answer,
            String riskLevel,
            Boolean shouldBookVet,
            List<String> recommendedActions,
            AiAction action
    ) {
    }

    public record AiPetChatExecution(
            Long userId,
            AiPetChatResponse response,
            AiPetChatSocketEventDTO userEvent,
            AiPetChatSocketEventDTO assistantEvent,
            AiPetChatSocketEventDTO responseEvent
    ) {
    }
}
