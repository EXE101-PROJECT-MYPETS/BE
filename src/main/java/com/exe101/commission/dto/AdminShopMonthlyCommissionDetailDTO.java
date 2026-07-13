package com.exe101.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminShopMonthlyCommissionDetailDTO {
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private AdminShopMonthlyCommissionDTO shop;
    private List<CommissionDTO> commissions;
    private List<CommissionInvoiceDTO> invoices;
}
