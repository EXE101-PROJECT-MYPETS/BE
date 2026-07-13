package com.exe101.commission.dto;

import com.exe101.common.PageResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommissionMonthlyReportDTO {
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private AdminCommissionMonthlySummaryDTO summary;
    private PageResponse<AdminShopMonthlyCommissionDTO> shops;
}
