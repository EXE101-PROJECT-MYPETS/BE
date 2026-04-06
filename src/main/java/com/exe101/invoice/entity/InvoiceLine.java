package com.exe101.invoice.entity;

import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoice_lines")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "line_type", nullable = false)
    private String lineType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(nullable = false)
    private Integer qty = 1;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice = 0L;

    @Column(nullable = false)
    private Long amount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "invoice_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private Invoice invoice;
}
