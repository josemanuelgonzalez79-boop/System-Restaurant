package com.rest.restaurantsystem.report;

import com.rest.restaurantsystem.business.BusinessSettingsResponse;
import com.rest.restaurantsystem.business.BusinessSettingsService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.order.PaymentMethod;
import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Service
public class DailyReportService {

    private static final Set<UserRole> REPORT_ROLES =
            EnumSet.of(UserRole.OWNER, UserRole.ADMIN, UserRole.MANAGER);

    private final DailyReportQueryRepository repository;
    private final BranchService branchService;
    private final BranchAssignmentService assignmentService;
    private final UserService userService;
    private final BusinessSettingsService businessSettingsService;
    private final Clock clock;

    @Autowired
    public DailyReportService(
            DailyReportQueryRepository repository,
            BranchService branchService,
            BranchAssignmentService assignmentService,
            UserService userService,
            BusinessSettingsService businessSettingsService
    ) {
        this(
                repository,
                branchService,
                assignmentService,
                userService,
                businessSettingsService,
                Clock.systemUTC()
        );
    }

    DailyReportService(
            DailyReportQueryRepository repository,
            BranchService branchService,
            BranchAssignmentService assignmentService,
            UserService userService,
            BusinessSettingsService businessSettingsService,
            Clock clock
    ) {
        this.repository = repository;
        this.branchService = branchService;
        this.assignmentService = assignmentService;
        this.userService = userService;
        this.businessSettingsService = businessSettingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DailyReportResponse findDaily(Long branchId, LocalDate date, String currentUsername) {
        BranchResponse branch = branchService.findById(branchId);
        authorize(branchId, currentUsername);

        ZoneId timezone = ZoneId.of(branch.timezone());
        LocalDate today = LocalDate.now(clock.withZone(timezone));
        if (date.isAfter(today)) {
            throw new BadRequestException("No es posible generar un reporte de una fecha futura.");
        }

        Instant start = date.atStartOfDay(timezone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(timezone).toInstant();
        BusinessSettingsResponse settings = businessSettingsService.get();

        List<DailyReportResponse.PaymentMethodSummary> paymentMethods =
                buildPaymentMethods(branchId, start, end);
        DailyReportResponse.SalesSummary sales = buildSalesSummary(paymentMethods);
        DailyReportResponse.TipsSummary tips = buildTipsSummary(
                repository.findTipMetrics(branchId, start, end));
        DailyReportResponse.OrderSummary orders = buildOrderSummary(
                repository.findOrderMetrics(branchId, start, end)
        );
        List<DailyReportResponse.HourlySalesSummary> hourlySales = buildHourlySales(
                branchId,
                start,
                end,
                branch.timezone()
        );
        List<DailyReportResponse.ProductSalesSummary> topProducts = buildTopProducts(
                branchId,
                start,
                end
        );
        DailyReportResponse.CashSummary cash = buildCashSummary(
                branchId,
                start,
                end,
                paymentMethods,
                tips
        );

        return new DailyReportResponse(
                branch.id(),
                branch.name(),
                branch.timezone(),
                settings.currencyCode(),
                date,
                start,
                end,
                clock.instant(),
                sales,
                tips,
                orders,
                paymentMethods,
                hourlySales,
                topProducts,
                cash,
                repository.findCancelledOrders(branchId, start, end),
                repository.findVoidedPayments(branchId, start, end)
        );
    }

    private void authorize(Long branchId, String currentUsername) {
        UserResponse user = userService.currentUser(currentUsername);
        if (!user.active()) {
            throw new BadRequestException("Tu usuario está inactivo.");
        }
        if (!REPORT_ROLES.contains(user.role())) {
            throw new BadRequestException(
                    "Solo propietario, administrador o gerente puede consultar reportes."
            );
        }
        if (!assignmentService.isAssigned(branchId, user.id())) {
            throw new BadRequestException(
                    "No tienes acceso a este restaurante."
            );
        }
    }

    private List<DailyReportResponse.PaymentMethodSummary> buildPaymentMethods(
            Long branchId,
            Instant start,
            Instant end
    ) {
        Map<PaymentMethod, DailyReportQueryRepository.PaymentMethodRow> found =
                new EnumMap<>(PaymentMethod.class);
        repository.findPaymentMethods(branchId, start, end)
                .forEach(row -> found.put(row.method(), row));

        return List.of(PaymentMethod.values()).stream()
                .map(method -> {
                    DailyReportQueryRepository.PaymentMethodRow row = found.get(method);
                    BigDecimal gross = row == null ? BigDecimal.ZERO : row.grossAmount();
                    BigDecimal voided = row == null ? BigDecimal.ZERO : row.voidedAmount();
                    return new DailyReportResponse.PaymentMethodSummary(
                            method,
                            gross,
                            voided,
                            gross.subtract(voided),
                            row == null ? 0 : row.receivedCount(),
                            row == null ? 0 : row.voidedCount()
                    );
                })
                .toList();
    }

    private DailyReportResponse.SalesSummary buildSalesSummary(
            List<DailyReportResponse.PaymentMethodSummary> paymentMethods
    ) {
        BigDecimal gross = paymentMethods.stream()
                .map(DailyReportResponse.PaymentMethodSummary::grossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal voided = paymentMethods.stream()
                .map(DailyReportResponse.PaymentMethodSummary::voidedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long receivedCount = paymentMethods.stream()
                .mapToLong(DailyReportResponse.PaymentMethodSummary::receivedCount)
                .sum();
        long voidedCount = paymentMethods.stream()
                .mapToLong(DailyReportResponse.PaymentMethodSummary::voidedCount)
                .sum();
        return new DailyReportResponse.SalesSummary(
                gross,
                voided,
                gross.subtract(voided),
                receivedCount,
                voidedCount
        );
    }

    private DailyReportResponse.TipsSummary buildTipsSummary(
            List<DailyReportQueryRepository.TipMetricRow> rows
    ) {
        Map<PaymentMethod, BigDecimal[]> methods = new EnumMap<>(PaymentMethod.class);
        Map<Long, BigDecimal[]> people = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal voided = BigDecimal.ZERO;
        for (DailyReportQueryRepository.TipMetricRow row : rows) {
            gross = gross.add(row.grossAmount());
            voided = voided.add(row.voidedAmount());
            BigDecimal[] method = methods.computeIfAbsent(row.method(), ignored ->
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            method[0] = method[0].add(row.grossAmount());
            method[1] = method[1].add(row.voidedAmount());
            BigDecimal[] person = people.computeIfAbsent(row.beneficiaryId(), ignored ->
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            person[0] = person[0].add(row.grossAmount());
            person[1] = person[1].add(row.voidedAmount());
            names.putIfAbsent(row.beneficiaryId(), row.beneficiaryName());
        }
        List<DailyReportResponse.TipMethodSummary> byMethod = List.of(PaymentMethod.values())
                .stream().map(method -> {
                    BigDecimal[] amounts = methods.getOrDefault(method,
                            new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    return new DailyReportResponse.TipMethodSummary(method, amounts[0],
                            amounts[1], amounts[0].subtract(amounts[1]));
                }).toList();
        List<DailyReportResponse.TipBeneficiarySummary> byPerson = people.entrySet().stream()
                .map(entry -> new DailyReportResponse.TipBeneficiarySummary(entry.getKey(),
                        names.get(entry.getKey()), entry.getValue()[0], entry.getValue()[1],
                        entry.getValue()[0].subtract(entry.getValue()[1])))
                .toList();
        return new DailyReportResponse.TipsSummary(gross, voided, gross.subtract(voided),
                byMethod, byPerson);
    }

    private DailyReportResponse.OrderSummary buildOrderSummary(
            DailyReportQueryRepository.OrderMetricsRow metrics
    ) {
        BigDecimal averageTicket = metrics.completedCount() == 0
                ? BigDecimal.ZERO
                : metrics.completedOrderValue().divide(
                        BigDecimal.valueOf(metrics.completedCount()),
                        2,
                        RoundingMode.HALF_UP
                );
        return new DailyReportResponse.OrderSummary(
                metrics.openedCount(),
                metrics.completedCount(),
                metrics.cancelledCount(),
                metrics.pendingAtEndCount(),
                metrics.dineInCount(),
                metrics.takeoutCount(),
                metrics.guestCount(),
                metrics.completedOrderValue(),
                averageTicket
        );
    }

    private List<DailyReportResponse.HourlySalesSummary> buildHourlySales(
            Long branchId,
            Instant start,
            Instant end,
            String timezone
    ) {
        Map<Integer, DailyReportResponse.HourlySalesSummary> found = repository
                .findHourlySales(branchId, start, end, timezone)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        DailyReportResponse.HourlySalesSummary::hour,
                        value -> value
                ));
        return IntStream.range(0, 24)
                .mapToObj(hour -> found.getOrDefault(
                        hour,
                        new DailyReportResponse.HourlySalesSummary(
                                hour,
                                BigDecimal.ZERO,
                                0,
                                0
                        )
                ))
                .toList();
    }

    private List<DailyReportResponse.ProductSalesSummary> buildTopProducts(
            Long branchId,
            Instant start,
            Instant end
    ) {
        List<DailyReportQueryRepository.ProductSalesRow> products =
                repository.findTopProducts(branchId, start, end);
        return IntStream.range(0, products.size())
                .mapToObj(index -> {
                    DailyReportQueryRepository.ProductSalesRow product = products.get(index);
                    return new DailyReportResponse.ProductSalesSummary(
                            index + 1,
                            product.productName(),
                            product.quantity(),
                            product.amount()
                    );
                })
                .toList();
    }

    private DailyReportResponse.CashSummary buildCashSummary(
            Long branchId,
            Instant start,
            Instant end,
            List<DailyReportResponse.PaymentMethodSummary> paymentMethods,
            DailyReportResponse.TipsSummary tips
    ) {
        DailyReportQueryRepository.CashMetricsRow metrics =
                repository.findCashMetrics(branchId, start, end);
        BigDecimal cashPayments = paymentMethods.stream()
                .filter(method -> method.method() == PaymentMethod.CASH)
                .map(DailyReportResponse.PaymentMethodSummary::netAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        BigDecimal cashTips = tips.methods().stream()
                .filter(method -> method.method() == PaymentMethod.CASH)
                .map(DailyReportResponse.TipMethodSummary::netAmount)
                .findFirst().orElse(BigDecimal.ZERO);
        BigDecimal cashInNet = metrics.cashInGross().subtract(metrics.cashInVoided());
        BigDecimal cashOutNet = metrics.cashOutGross().subtract(metrics.cashOutVoided());
        BigDecimal expectedCashFlow = metrics.openingAmount()
                .add(cashPayments)
                .add(cashTips)
                .add(cashInNet)
                .subtract(cashOutNet);

        return new DailyReportResponse.CashSummary(
                metrics.openedSessionCount(),
                metrics.closedSessionCount(),
                metrics.openAtEndCount(),
                metrics.openingAmount(),
                cashPayments,
                cashTips,
                metrics.cashInGross(),
                metrics.cashInVoided(),
                cashInNet,
                metrics.cashOutGross(),
                metrics.cashOutVoided(),
                cashOutNet,
                expectedCashFlow,
                metrics.closingDifference(),
                repository.findCashSessions(branchId, start, end)
        );
    }
}
