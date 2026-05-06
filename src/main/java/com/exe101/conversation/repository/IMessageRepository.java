package com.exe101.conversation.repository;

import com.exe101.conversation.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IMessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);
}
