package com.exe101.commission.service;

import com.exe101.commission.dto.AdminCommissionCollectionStatus;
import com.exe101.commission.dto.AdminCommissionMonthlyReportDTO;
import com.exe101.commission.entity.CommissionInvoiceStatus;
import com.exe101.commission.entity.CommissionStatus;
import com.exe101.commission.repository.AdminShopMonthlyCommissionProjection;
import com.exe101.commission.repository.IPlatformCommissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionInvoiceServiceMonthlyReportTest {

    @Mock
    private IPlatformCommissionRepository commissionRepository;

    @InjectMocks
    private CommissionInvoiceService service;

    @Mock
    private AdminShopMonthlyCommissionProjection overdueShop;

    @Mock
    private AdminShopMonthlyCommissionProjection paidShop;

    @Test
    void monthlyReportBuildsSummaryBeforeApplyingTableFilters() {
        mockOverdueShop();
        mockPaidShop();
        when(commissionRepository.findAdminMonthlyCommissionRows(
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(CommissionStatus.class),
                any(CommissionStatus.class),
                any(CommissionStatus.class),
                any(CommissionInvoiceStatus.class),
                anyList()
        )).thenReturn(List.of(overdueShop, paidShop));

        AdminCommissionMonthlyReportDTO report = service.getAdminMonthlyReport(
                "2026-07",
                null,
                AdminCommissionCollectionStatus.OVERDUE,
                0,
                20
        );

        assertEquals("2026-07-01", report.getPeriodFrom().toString());
        assertEquals("2026-07-31", report.getPeriodTo().toString());
        assertEquals(2L, report.getSummary().getShopCount());
        assertEquals(7L, report.getSummary().getTransactionCount());
        assertEquals(1L, report.getSummary().getOutstandingShopCount());
        assertEquals(1L, report.getSummary().getOverdueShopCount());
        assertEquals(1_400L, report.getSummary().getCommissionAmount());
        assertEquals(500L, report.getSummary().getOutstandingAmount());
        assertEquals(900L, report.getSummary().getCollectedAmount());
        assertEquals(1L, report.getShops().getTotalElements());
        assertEquals(1L, report.getShops().getContent().get(0).getShopId());
        assertEquals(AdminCommissionCollectionStatus.OVERDUE, report.getShops().getContent().get(0).getStatus());
    }

    @Test
    void monthlyReportRejectsInvalidMonth() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getAdminMonthlyReport("07-2026", null, null, 0, 20)
        );

        assertEquals("Tháng báo cáo không hợp lệ, định dạng đúng là yyyy-MM", exception.getMessage());
    }

    private void mockOverdueShop() {
        when(overdueShop.getShopId()).thenReturn(1L);
        when(overdueShop.getShopName()).thenReturn("Pawly Quận 1");
        when(overdueShop.getTransactionCount()).thenReturn(4L);
        when(overdueShop.getInvoiceCount()).thenReturn(1L);
        when(overdueShop.getGrossAmount()).thenReturn(8_000L);
        when(overdueShop.getCommissionBase()).thenReturn(6_000L);
        when(overdueShop.getCommissionAmount()).thenReturn(1_000L);
        when(overdueShop.getPendingAmount()).thenReturn(300L);
        when(overdueShop.getInvoicedAmount()).thenReturn(200L);
        when(overdueShop.getCollectedAmount()).thenReturn(500L);
        when(overdueShop.getOverdueAmount()).thenReturn(200L);
    }

    private void mockPaidShop() {
        when(paidShop.getShopId()).thenReturn(2L);
        when(paidShop.getShopName()).thenReturn("Pawly Thủ Đức");
        when(paidShop.getTransactionCount()).thenReturn(3L);
        when(paidShop.getInvoiceCount()).thenReturn(1L);
        when(paidShop.getGrossAmount()).thenReturn(3_000L);
        when(paidShop.getCommissionBase()).thenReturn(2_000L);
        when(paidShop.getCommissionAmount()).thenReturn(400L);
        when(paidShop.getPendingAmount()).thenReturn(0L);
        when(paidShop.getInvoicedAmount()).thenReturn(0L);
        when(paidShop.getCollectedAmount()).thenReturn(400L);
        when(paidShop.getOverdueAmount()).thenReturn(0L);
    }
}
