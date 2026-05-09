package com.exe101.order.entity;

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
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_address_id")
    private Long customerAddressId;

    @Column(name = "user_address_id")
    private Long userAddressId;

    @Column(name = "order_code")
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSource source = OrderSource.ONLINE;

    @Column(name = "subtotal_amount", nullable = false)
    private Long subtotalAmount = 0L;

    @Column(name = "shipping_fee", nullable = false)
    private Long shippingFee = 0L;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "shipping_province")
    private String shippingProvince;

    @Column(name = "shipping_district")
    private String shippingDistrict;

    @Column(name = "shipping_ward")
    private String shippingWard;

    @Column(name = "shipping_street")
    private String shippingStreet;

    @Column(name = "shipping_hamlet")
    private String shippingHamlet;

    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private Customer customer;
}
