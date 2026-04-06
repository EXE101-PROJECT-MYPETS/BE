package com.exe101.conversation.entity;

import com.exe101.customer.entity.Customer;
import com.exe101.shop.entity.Shop;
import com.exe101.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private MessageSenderType senderType;

    @Column(name = "sender_customer_id")
    private Long senderCustomerId;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)
    @JsonIgnore
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_customer_id", insertable = false, updatable = false)
    @JsonIgnore
    private Customer senderCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User senderUser;
}
