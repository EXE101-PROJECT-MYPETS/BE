package com.exe101.commission.repository;

public interface AdminShopMonthlyCommissionProjection {
    Long getShopId();

    String getShopName();

    String getShopImageUrl();

    Long getTransactionCount();

    Long getInvoiceCount();

    Long getGrossAmount();

    Long getCommissionBase();

    Long getCommissionAmount();

    Long getPendingAmount();

    Long getInvoicedAmount();

    Long getCollectedAmount();

    Long getOverdueAmount();
}
