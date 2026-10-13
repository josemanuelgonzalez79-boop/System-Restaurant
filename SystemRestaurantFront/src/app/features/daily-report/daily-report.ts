import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { auditTime, finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import {
  DailyPaymentMethodSummary,
  DailyReport,
  HourlySalesSummary,
} from '../../core/models/daily-report.model';
import { PaymentMethod } from '../../core/models/payment.model';
import { Branch } from '../../core/models/structure.model';
import { DailyReportApiService } from '../../core/services/daily-report-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';

@Component({
  selector: 'app-daily-report',
  imports: [ButtonModule, CurrencyPipe, DatePipe, DecimalPipe, TagModule],
  templateUrl: './daily-report.html',
  styleUrl: './daily-report.scss',
})
export class DailyReportPage implements OnInit, OnDestroy {
  private readonly orderApi = inject(OrderApiService);
  private readonly reportApi = inject(DailyReportApiService);
  private readonly realtime = inject(RealtimeService);
  private realtimeSubscription?: Subscription;
  private requestActive = false;
  private pendingRealtimeRefresh = false;

  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly selectedBranchId = signal<number | null>(null);
  protected readonly selectedDate = signal(this.localDate(new Date()));
  protected readonly maximumDate = this.localDate(new Date());
  protected readonly report = signal<DailyReport | null>(null);
  protected readonly loadError = signal<string | null>(null);

  protected readonly maxHourlyAmount = computed(() =>
    Math.max(1, ...(this.report()?.hourlySales.map((item) => Math.abs(item.netAmount)) ?? [0])),
  );
  protected readonly maxProductQuantity = computed(() =>
    Math.max(1, ...(this.report()?.topProducts.map((item) => item.quantity) ?? [0])),
  );
  protected readonly hasHourlyActivity = computed(() =>
    Boolean(
      this.report()?.hourlySales.some(
        (item) => item.netAmount !== 0 || item.receivedCount > 0 || item.voidedCount > 0,
      ),
    ),
  );

  ngOnInit(): void {
    this.orderApi
      .findBranches()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((branches) => {
        this.branches.set(branches);
        this.changeBranch(branches[0]?.id ?? null);
      });
  }

  ngOnDestroy(): void {
    this.realtimeSubscription?.unsubscribe();
  }

  protected selectBranch(value: string): void {
    const branchId = Number(value);
    this.changeBranch(Number.isInteger(branchId) && branchId > 0 ? branchId : null);
  }

  protected selectDate(value: string): void {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return;
    }
    this.selectedDate.set(value);
    this.report.set(null);
    this.loadReport(false);
  }

  protected refresh(): void {
    this.loadReport(false);
  }

  protected printReport(): void {
    window.print();
  }

  protected exportCsv(): void {
    const report = this.report();
    if (!report) {
      return;
    }

    const rows: Array<Array<string | number | null>> = [
      ['Reporte diario', report.branchName, report.businessDate],
      ['Zona horaria', report.timezone],
      [],
      ['RESUMEN DE VENTAS'],
      ['Cobros recibidos', report.sales.grossCollected],
      ['Anulaciones del día', report.sales.voidedAmount],
      ['Venta neta', report.sales.netCollected],
      ['Pedidos abiertos', report.orders.openedCount],
      ['Pedidos completados', report.orders.completedCount],
      ['Pedidos cancelados', report.orders.cancelledCount],
      ['Ticket promedio', report.orders.averageTicket],
      [],
      ['FORMAS DE PAGO'],
      ['Forma', 'Cobrado', 'Anulado', 'Neto', 'Operaciones', 'Anulaciones'],
      ...report.paymentMethods.map((method) => [
        this.paymentMethodLabel(method.method),
        method.grossAmount,
        method.voidedAmount,
        method.netAmount,
        method.receivedCount,
        method.voidedCount,
      ]),
      [],
      ['VENTA POR HORA'],
      ['Hora', 'Venta neta', 'Cobros', 'Anulaciones'],
      ...report.hourlySales.map((hour) => [
        this.hourLabel(hour.hour),
        hour.netAmount,
        hour.receivedCount,
        hour.voidedCount,
      ]),
      [],
      ['PRODUCTOS MÁS VENDIDOS'],
      ['Posición', 'Producto', 'Cantidad', 'Importe'],
      ...report.topProducts.map((product) => [
        product.rank,
        product.productName,
        product.quantity,
        product.amount,
      ]),
      [],
      ['CAJA'],
      ['Fondos iniciales', report.cash.openingAmount],
      ['Efectivo cobrado', report.cash.cashPayments],
      ['Entradas netas', report.cash.cashInNet],
      ['Salidas netas', report.cash.cashOutNet],
      ['Flujo esperado', report.cash.expectedCashFlow],
      ['Diferencia de cortes', report.cash.closingDifference],
      [],
      ['PEDIDOS CANCELADOS'],
      ['Folio', 'Modalidad', 'Referencia', 'Responsable', 'Fecha', 'Importe'],
      ...report.cancelledOrders.map((order) => [
        order.folio,
        this.serviceModeLabel(order.serviceMode),
        order.customerReference,
        order.assignedUserName,
        order.cancelledAt,
        order.amount,
      ]),
      [],
      ['COBROS ANULADOS'],
      ['Folio', 'Pedido', 'Forma', 'Importe', 'Fecha', 'Usuario', 'Motivo'],
      ...report.voidedPayments.map((payment) => [
        payment.folio,
        payment.orderFolio,
        this.paymentMethodLabel(payment.method),
        payment.amount,
        payment.voidedAt,
        payment.voidedByUserName,
        payment.reason,
      ]),
    ];

    const csv = `\uFEFF${rows.map((row) => row.map((value) => this.csvCell(value)).join(',')).join('\r\n')}`;
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    const safeBranch = report.branchName.replace(/[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ-]+/g, '-');
    link.href = url;
    link.download = `reporte-diario-${safeBranch}-${report.businessDate}.csv`;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  protected paymentMethodLabel(method: PaymentMethod): string {
    const labels: Record<PaymentMethod, string> = {
      CASH: 'Efectivo',
      CARD: 'Tarjeta',
      TRANSFER: 'Transferencia',
      OTHER: 'Otro',
    };
    return labels[method];
  }

  protected serviceModeLabel(mode: 'DINE_IN' | 'TAKEOUT'): string {
    return mode === 'DINE_IN' ? 'En mesa' : 'Para llevar';
  }

  protected hourLabel(hour: number): string {
    return `${hour.toString().padStart(2, '0')}:00`;
  }

  protected instantLabel(value: string | null, timezone: string): string {
    if (!value) {
      return '—';
    }
    return new Intl.DateTimeFormat('es-MX', {
      dateStyle: 'short',
      timeStyle: 'short',
      timeZone: timezone,
    }).format(new Date(value));
  }

  protected hourlyBarHeight(hour: HourlySalesSummary): number {
    if (hour.netAmount === 0) {
      return 0;
    }
    return Math.max(6, (Math.abs(hour.netAmount) / this.maxHourlyAmount()) * 100);
  }

  protected productBarWidth(quantity: number): number {
    return (quantity / this.maxProductQuantity()) * 100;
  }

  protected paymentBarWidth(method: DailyPaymentMethodSummary): number {
    const maximum = Math.max(
      1,
      ...(this.report()?.paymentMethods.map((item) => Math.abs(item.netAmount)) ?? [0]),
    );
    return (Math.abs(method.netAmount) / maximum) * 100;
  }

  private changeBranch(branchId: number | null): void {
    this.realtimeSubscription?.unsubscribe();
    this.realtimeSubscription = undefined;
    this.pendingRealtimeRefresh = false;
    this.selectedBranchId.set(branchId);
    this.report.set(null);
    this.loadError.set(null);
    if (branchId !== null) {
      this.realtimeSubscription = this.realtime
        .watchBranch(branchId)
        .pipe(auditTime(500))
        .subscribe(() => this.loadReport(true));
      this.loadReport(false);
    }
  }

  private loadReport(silent: boolean): void {
    const branchId = this.selectedBranchId();
    const date = this.selectedDate();
    if (branchId === null) {
      return;
    }
    if (this.requestActive) {
      this.pendingRealtimeRefresh = true;
      return;
    }

    this.requestActive = true;
    this.loadError.set(null);
    if (!silent) {
      this.refreshing.set(true);
    }
    this.reportApi
      .findDaily(branchId, date)
      .pipe(
        finalize(() => {
          this.requestActive = false;
          this.loading.set(false);
          if (!silent) {
            this.refreshing.set(false);
          }
          if (this.pendingRealtimeRefresh) {
            this.pendingRealtimeRefresh = false;
            this.loadReport(true);
          }
        }),
      )
      .subscribe({
        next: (report) => {
          if (this.selectedBranchId() === branchId && this.selectedDate() === date) {
            this.report.set(report);
          }
        },
        error: () => {
          if (this.selectedBranchId() === branchId && this.selectedDate() === date) {
            this.loadError.set('No fue posible generar el reporte seleccionado.');
          }
        },
      });
  }

  private localDate(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private csvCell(value: string | number | null): string {
    if (value === null || value === undefined) {
      return '';
    }
    const raw = typeof value === 'number' ? value.toFixed(2) : String(value);
    const text = typeof value === 'string' && /^[=+\-@\t\r]/.test(raw) ? `'${raw}` : raw;
    return `"${text.replaceAll('"', '""')}"`;
  }
}
