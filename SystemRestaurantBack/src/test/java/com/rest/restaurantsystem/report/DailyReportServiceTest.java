package com.rest.restaurantsystem.report;

import com.rest.restaurantsystem.business.BusinessSettingsResponse;
import com.rest.restaurantsystem.business.BusinessSettingsService;
import com.rest.restaurantsystem.business.BusinessType;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.order.PaymentMethod;
import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {

    @Mock
    private DailyReportQueryRepository repository;

    @Mock
    private BranchService branchService;

    @Mock
    private BranchAssignmentService assignmentService;

    @Mock
    private UserService userService;

    @Mock
    private BusinessSettingsService businessSettingsService;

    private DailyReportService service;

    @BeforeEach
    void setUp() {
        service = new DailyReportService(
                repository,
                branchService,
                assignmentService,
                userService,
                businessSettingsService,
                Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void buildsTheReportUsingTheBranchCalendarDay() {
        mockAuthorizedManager();
        when(businessSettingsService.get()).thenReturn(settings());
        when(repository.findPaymentMethods(1L, start(), end())).thenReturn(List.of(
                new DailyReportQueryRepository.PaymentMethodRow(
                        PaymentMethod.CASH,
                        new BigDecimal("1250.00"),
                        new BigDecimal("100.00"),
                        8,
                        1
                ),
                new DailyReportQueryRepository.PaymentMethodRow(
                        PaymentMethod.CARD,
                        new BigDecimal("500.00"),
                        BigDecimal.ZERO,
                        2,
                        0
                )
        ));
        when(repository.findTipMetrics(1L, start(), end())).thenReturn(List.of(
                new DailyReportQueryRepository.TipMetricRow(
                        PaymentMethod.CASH, 21L, "Ana", new BigDecimal("20.00"),
                        new BigDecimal("5.00")),
                new DailyReportQueryRepository.TipMetricRow(
                        PaymentMethod.CARD, 21L, "Ana", new BigDecimal("10.00"),
                        BigDecimal.ZERO)
        ));
        when(repository.findOrderMetrics(1L, start(), end())).thenReturn(
                new DailyReportQueryRepository.OrderMetricsRow(
                        11,
                        10,
                        1,
                        0,
                        8,
                        3,
                        28,
                        new BigDecimal("1650.00")
                )
        );
        when(repository.findHourlySales(1L, start(), end(), "America/Mazatlan"))
                .thenReturn(List.of(
                        new DailyReportResponse.HourlySalesSummary(
                                14,
                                new BigDecimal("600.00"),
                                3,
                                0
                        )
                ));
        when(repository.findTopProducts(1L, start(), end())).thenReturn(List.of(
                new DailyReportQueryRepository.ProductSalesRow(
                        "Hamburguesa clásica",
                        6,
                        new BigDecimal("720.00")
                )
        ));
        when(repository.findCashMetrics(1L, start(), end())).thenReturn(
                new DailyReportQueryRepository.CashMetricsRow(
                        1,
                        1,
                        0,
                        new BigDecimal("500.00"),
                        new BigDecimal("-5.00"),
                        new BigDecimal("50.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("20.00"),
                        BigDecimal.ZERO
                )
        );
        when(repository.findCashSessions(1L, start(), end())).thenReturn(List.of());
        when(repository.findCancelledOrders(1L, start(), end())).thenReturn(List.of());
        when(repository.findVoidedPayments(1L, start(), end())).thenReturn(List.of());

        DailyReportResponse report = service.findDaily(
                1L,
                LocalDate.parse("2026-09-14"),
                "gerente"
        );

        assertThat(report.periodStart()).isEqualTo(start());
        assertThat(report.periodEnd()).isEqualTo(end());
        assertThat(report.sales().netCollected()).isEqualByComparingTo("1650.00");
        assertThat(report.tips().netAmount()).isEqualByComparingTo("25.00");
        assertThat(report.tips().beneficiaries().getFirst().netAmount())
                .isEqualByComparingTo("25.00");
        assertThat(report.cash().cashTips()).isEqualByComparingTo("15.00");
        assertThat(report.orders().averageTicket()).isEqualByComparingTo("165.00");
        assertThat(report.hourlySales()).hasSize(24);
        assertThat(report.hourlySales().get(14).netAmount()).isEqualByComparingTo("600.00");
        assertThat(report.topProducts().getFirst().rank()).isEqualTo(1);
        assertThat(report.cash().expectedCashFlow()).isEqualByComparingTo("1695.00");
    }

    @Test
    void rejectsAUserThatIsNotAssignedToTheBranch() {
        when(branchService.findById(1L)).thenReturn(branch());
        when(userService.currentUser("gerente")).thenReturn(manager());
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.findDaily(1L, LocalDate.parse("2026-09-14"), "gerente")
        );

        assertThat(exception.getMessage()).contains("no está asignado");
        verify(repository, never()).findOrderMetrics(eq(1L), any(), any());
    }

    @Test
    void rejectsAFutureDateInTheBranchTimezone() {
        mockAuthorizedManager();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.findDaily(1L, LocalDate.parse("2026-09-16"), "gerente")
        );

        assertThat(exception.getMessage()).contains("fecha futura");
        verify(repository, never()).findOrderMetrics(eq(1L), any(), any());
    }

    private void mockAuthorizedManager() {
        when(branchService.findById(1L)).thenReturn(branch());
        when(userService.currentUser("gerente")).thenReturn(manager());
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);
    }

    private Instant start() {
        return Instant.parse("2026-09-14T07:00:00Z");
    }

    private Instant end() {
        return Instant.parse("2026-09-15T07:00:00Z");
    }

    private UserResponse manager() {
        return new UserResponse(
                10L,
                "gerente",
                "Gerente principal",
                UserRole.MANAGER,
                true,
                false,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private BranchResponse branch() {
        return new BranchResponse(
                1L,
                "PRINCIPAL",
                "Sucursal principal",
                null,
                null,
                "America/Mazatlan",
                0,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private BusinessSettingsResponse settings() {
        return new BusinessSettingsResponse(
                1L,
                "Restaurante Essential",
                "Essential",
                BusinessType.RESTAURANT,
                "MXN",
                "America/Mazatlan",
                "#0f766e",
                "#f59e0b",
                null,
                null,
                0,
                Instant.EPOCH
        );
    }
}
