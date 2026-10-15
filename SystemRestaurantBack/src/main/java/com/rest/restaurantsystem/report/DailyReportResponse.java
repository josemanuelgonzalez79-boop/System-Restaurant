package com.rest.restaurantsystem.report;

import com.rest.restaurantsystem.order.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        Long branchId,
        String branchName,
        String timezone,
        String currencyCode,
        LocalDate businessDate,
        Instant periodStart,
        Instant periodEnd,
        Instant generatedAt,
        SalesSummary sales,
        TipsSummary tips,
        OrderSummary orders,
        List<PaymentMethodSummary> paymentMethods,
        List<HourlySalesSummary> hourlySales,
        List<ProductSalesSummary> topProducts,
        CashSummary cash,
        List<CancelledOrderSummary> cancelledOrders,
        List<VoidedPaymentSummary> voidedPayments
) {

    public record SalesSummary(
            BigDecimal grossCollected,
            BigDecimal voidedAmount,
            BigDecimal netCollected,
            long receivedPaymentCount,
            long voidedPaymentCount
    ) {
    }

    public record TipsSummary(
            BigDecimal grossAmount,
            BigDecimal voidedAmount,
            BigDecimal netAmount,
            List<TipMethodSummary> methods,
            List<TipBeneficiarySummary> beneficiaries
    ) { }

    public record TipMethodSummary(PaymentMethod method, BigDecimal grossAmount,
                                   BigDecimal voidedAmount, BigDecimal netAmount) { }

    public record TipBeneficiarySummary(Long userId, String userName, BigDecimal grossAmount,
                                        BigDecimal voidedAmount, BigDecimal netAmount) { }

    public record OrderSummary(
            long openedCount,
            long completedCount,
            long cancelledCount,
            long pendingAtEndCount,
            long dineInCount,
            long takeoutCount,
            long guestCount,
            BigDecimal completedOrderValue,
            BigDecimal averageTicket
    ) {
    }

    public record PaymentMethodSummary(
            PaymentMethod method,
            BigDecimal grossAmount,
            BigDecimal voidedAmount,
            BigDecimal netAmount,
            long receivedCount,
            long voidedCount
    ) {
    }

    public record HourlySalesSummary(
            int hour,
            BigDecimal netAmount,
            long receivedCount,
            long voidedCount
    ) {
    }

    public record ProductSalesSummary(
            int rank,
            String productName,
            long quantity,
            BigDecimal amount
    ) {
    }

    public record CashSummary(
            long openedSessionCount,
            long closedSessionCount,
            long openAtEndCount,
            BigDecimal openingAmount,
            BigDecimal cashPayments,
            BigDecimal cashTips,
            BigDecimal cashInGross,
            BigDecimal cashInVoided,
            BigDecimal cashInNet,
            BigDecimal cashOutGross,
            BigDecimal cashOutVoided,
            BigDecimal cashOutNet,
            BigDecimal expectedCashFlow,
            BigDecimal closingDifference,
            List<CashSessionSummary> sessions
    ) {
    }

    public record CashSessionSummary(
            Long id,
            String folio,
            String status,
            String openedByUserName,
            Instant openedAt,
            String closedByUserName,
            Instant closedAt,
            BigDecimal openingAmount,
            BigDecimal expectedCashAtClose,
            BigDecimal countedCashAtClose,
            BigDecimal differenceAtClose
    ) {
    }

    public record CancelledOrderSummary(
            Long orderId,
            String folio,
            String serviceMode,
            String customerReference,
            String assignedUserName,
            Instant cancelledAt,
            BigDecimal amount
    ) {
    }

    public record VoidedPaymentSummary(
            Long paymentId,
            String folio,
            Long orderId,
            String orderFolio,
            PaymentMethod method,
            BigDecimal amount,
            Instant voidedAt,
            String voidedByUserName,
            String reason
    ) {
    }
}
