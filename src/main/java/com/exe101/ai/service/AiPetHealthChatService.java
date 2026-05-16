package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeSearchResult;
import com.exe101.ai.dto.AiPetChatConversationDTO;
import com.exe101.ai.dto.AiPetChatMessageDTO;
import com.exe101.ai.dto.AiPetChatRequest;
import com.exe101.ai.dto.AiPetChatResponse;
import com.exe101.ai.dto.AiPetChatSocketEventDTO;
import com.exe101.ai.dto.PetContext;
import com.exe101.ai.entity.AiPetChatConversation;
import com.exe101.ai.entity.AiPetChatMessage;
import com.exe101.ai.enums.AiChatRole;
import com.exe101.ai.exception.AiAccessDenied;
import com.exe101.ai.exception.AiNotFound;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.pet.entity.Pet;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.exe101.ai.repository.AiPetChatConversationRepository;
import com.exe101.ai.repository.AiPetChatMessageRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPetHealthChatService {

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
        AiResponsePayload payload = parseAiResponse(rawAnswer);

        AiPetChatMessage assistantMessage = new AiPetChatMessage();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole(AiChatRole.ASSISTANT);
        assistantMessage.setContent(payload.answer());
        assistantMessage.setMetadata(buildAssistantMetadata(payload));
        messageRepository.save(assistantMessage);

        conversationRepository.touch(conversation.getId());

        AiPetChatResponse response = new AiPetChatResponse(
                conversation.getId(),
                payload.answer(),
                payload.riskLevel(),
                payload.shouldBookVet(),
                payload.recommendedActions()
        );
        AiPetChatMessageDTO userMessageDTO = toMessageDTO(userMessage);
        AiPetChatMessageDTO assistantMessageDTO = toMessageDTO(assistantMessage);
        return new AiPetChatExecution(
                currentUserId,
                response,
                new AiPetChatSocketEventDTO("USER_MESSAGE", conversation.getId(), currentUserId, userMessageDTO, null),
                new AiPetChatSocketEventDTO("ASSISTANT_MESSAGE", conversation.getId(), currentUserId, assistantMessageDTO, null),
                new AiPetChatSocketEventDTO("CHAT_RESPONSE", conversation.getId(), currentUserId, null, response)
        );
    }

    @Transactional(readOnly = true)
    public List<AiPetChatConversationDTO> getConversations(Long petId) {
        Long currentUserId = getCurrentUserId();
        Pet pet = aiPetContextService.getAuthorizedPet(petId, currentUserId);
        return conversationRepository.findByPetIdOrderByUpdatedAtDesc(pet.getId()).stream()
                .map(this::toConversationDTO)
                .toList();
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
            AiPetChatConversation conversation = new AiPetChatConversation();
            conversation.setPetId(pet.getId());
            conversation.setUserId(currentUserId);
            conversation.setTitle(buildTitle(request.getMessage()));
            return conversationRepository.save(conversation);
        }

        AiPetChatConversation conversation = conversationRepository.findByIdAndPetId(request.getConversationId(), pet.getId())
                .orElseThrow(() -> new AiNotFound("AiConversationNotFound", "Không tìm thấy cuộc trò chuyện AI"));
        validateConversationOwnership(conversation, currentUserId);
        return conversation;
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
        return metadata;
    }

    private AiResponsePayload parseAiResponse(String rawAnswer) {
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
                    actions
            );
        } catch (Exception ex) {
            return new AiResponsePayload(rawAnswer, "MEDIUM", false, List.of());
        }
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
            List<String> recommendedActions
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
