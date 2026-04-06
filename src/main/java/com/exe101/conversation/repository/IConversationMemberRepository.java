package com.exe101.conversation.repository;

import com.exe101.conversation.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
}
