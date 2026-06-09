package com.exe101.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionInvoiceGenerateResponse {
    private int generatedCount;
    private List<CommissionInvoiceDTO> invoices;
}
