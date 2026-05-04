package com.exe101.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineDTO {
    private Long id;
    private Long shopId;
    private Long invoiceId;
    private String lineType;
    private Long refId;
    private String itemName;
    private Integer qty;
    private Long unitPrice;
    private Long amount;
}
