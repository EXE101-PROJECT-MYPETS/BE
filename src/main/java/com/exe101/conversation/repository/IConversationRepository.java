package com.exe101.conversation.repository;

import com.exe101.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IConversationRepository extends JpaRepository<Conversation, Long> {
}
