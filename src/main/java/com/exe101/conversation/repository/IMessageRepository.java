package com.exe101.conversation.repository;

import com.exe101.conversation.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IMessageRepository extends JpaRepository<Message, Long> {
}
