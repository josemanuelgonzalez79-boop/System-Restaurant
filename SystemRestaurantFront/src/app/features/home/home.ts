import { CurrencyPipe, DatePipe, DecimalPipe, registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { auditTime, finalize, forkJoin, interval, of, Subscription } from 'rxjs';

import { CashRegisterSummary } from '../../core/models/cash-register.model';
import { DailyReport } from '../../core/models/daily-report.model';
import {
  InventoryReport,
  InventoryReportIngredient,
} from '../../core/models/inventory-report.model';
import { RestaurantOrder } from '../../core/models/order.model';
import { PreparationTicket } from '../../core/models/preparation.model';
import { Promotion } from '../../core/models/promotion.model';
import { Reservation } from '../../core/models/reservation.model';
import { Branch } from '../../core/models/structure.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { CashRegisterApiService } from '../../core/services/cash-register-api.service';
import { DailyReportApiService } from '../../core/services/daily-report-api.service';
import { InventoryReportApiService } from '../../core/services/inventory-report-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { PreparationApiService } from '../../core/services/preparation-api.service';
import { PromotionApiService } from '../../core/services/promotion-api.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { ReservationApiService } from '../../core/services/reservation-api.service';

registerLocaleData(localeEsMx);

@Component({
  selector: 'app-home',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit, OnDestroy {
  private readonly orderApi = inject(OrderApiService);
  private readonly preparationApi = inject(PreparationApiService);
  private readonly cashApi = inject(CashRegisterApiService);
  private readonly inventoryApi = inject(InventoryReportApiService);
  private readonly reservationApi = inject(ReservationApiService);
  private readonly promotionApi = inject(PromotionApiService);
  private readonly reportApi = inject(DailyReportApiService);
  private readonly realtime = inject(RealtimeService);
  protected readonly auth = inject(AuthApiService);

  private readonly subscriptions = new Subscription();
  private branchWatch?: Subscription;
  private pendingRefresh = false;
  private pendingInventoryRefresh = false;
  private pendingAgendaRefresh = false;
  private pendingPromotionRefresh = false;
  private pendingTipsRefresh = false;
  private destroyed = false;

  protected readonly branch = signal<Branch | null>(null);
  protected readonly orders = signal<RestaurantOrder[]>([]);
  protected readonly tickets = signal<PreparationTicket[]>([]);
  protected readonly cash = signal<CashRegisterSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly loaded = signal(false);
  protected readonly refreshing = signal(false);
  protected readonly error = signal(false);
  protected readonly updatedAt = signal<Date | null>(null);
  protected readonly inventoryReport = signal<InventoryReport | null>(null);
  protected readonly inventoryLoading = signal(false);
  protected readonly inventoryError = signal(false);
  protected readonly inventoryUpdatedAt = signal<Date | null>(null);
  protected readonly reservations = signal<Reservation[]>([]);
  protected readonly agendaLoaded = signal(false);
  protected readonly agendaLoading = signal(false);
  protected readonly agendaError = signal(false);
  protected readonly promotions = signal<Promotion[]>([]);
  protected readonly promotionsLoaded = signal(false);
  protected readonly promotionsLoading = signal(false);
  protected readonly promotionsError = signal(false);
  protected readonly dailyReport = signal<DailyReport | null>(null);
  protected readonly tipsLoading = signal(false);
  protected readonly tipsError = signal(false);

  protected readonly occupiedTables = computed(
    () =>
      new Set(
        this.orders()
          .filter((order) => order.serviceMode === 'DINE_IN' && order.servicePointId !== null)
          .map((order) => order.servicePointId),
      ).size,
  );
  protected readonly pendingItems = computed(() =>
    this.tickets().reduce(
      (total, ticket) => total + ticket.items.filter((item) => item.status === 'PENDING').length,
      0,
    ),
  );
  protected readonly preparingItems = computed(() =>
    this.tickets().reduce(
      (total, ticket) =>
        total + ticket.items.filter((item) => item.status === 'IN_PREPARATION').length,
      0,
    ),
  );
  protected readonly readyItems = computed(() =>
    this.tickets().reduce(
      (total, ticket) => total + ticket.items.filter((item) => item.status === 'READY').length,
      0,
    ),
  );
  protected readonly recentOrders = computed(() =>
    [...this.orders()]
      .sort((a, b) => new Date(b.openedAt).getTime() - new Date(a.openedAt).getTime())
      .slice(0, 5),
  );
  protected readonly stockAlerts = computed(
    () =>
      this.inventoryReport()?.ingredients.filter(
        (ingredient) => ingredient.active && (ingredient.lowStock || ingredient.outOfStock),
      ).length ?? 0,
  );
  protected readonly attentionItems = computed(() =>
    (this.inventoryReport()?.ingredients ?? [])
      .filter(
        (ingredient) =>
          ingredient.active &&
          (ingredient.lowStock || ingredient.outOfStock || ingredient.unitCost === null),
      )
      .sort(
        (a, b) =>
          Number(b.outOfStock) - Number(a.outOfStock) ||
          Number(b.lowStock) - Number(a.lowStock) ||
          a.name.localeCompare(b.name, 'es'),
      )
      .slice(0, 5),
  );
  protected readonly bookedReservations = computed(() =>
    this.reservations().filter((reservation) => reservation.status === 'BOOKED'),
  );
  protected readonly seatedReservations = computed(
    () => this.reservations().filter((reservation) => reservation.status === 'SEATED').length,
  );
  protected readonly nextReservations = computed(() =>
    [...this.bookedReservations()]
      .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime())
      .slice(0, 4),
  );

  ngOnInit(): void {
    this.loadBranch();
    this.subscriptions.add(
      interval(60_000).subscribe(() => {
        if (this.branch()) {
          this.refresh();
        }
      }),
    );
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.branchWatch?.unsubscribe();
    this.subscriptions.unsubscribe();
  }

  protected retry(): void {
    if (this.branch()) {
      this.refresh();
    } else {
      this.loadBranch();
    }
  }

  protected refresh(): void {
    const branch = this.branch();
    if (!branch) {
      return;
    }
    if (this.refreshing()) {
      this.pendingRefresh = true;
      return;
    }
    this.refreshAdditional();
    this.refreshing.set(true);
    this.error.set(false);
    const request = forkJoin({
      orders: this.orderApi.findOrders(branch.id),
      tickets: this.preparationApi.findTickets(branch.id),
      cash: this.auth.canOperateCash() ? this.cashApi.findCurrent(branch.id) : of(null),
    })
      .pipe(
        finalize(() => {
          this.refreshing.set(false);
          this.loading.set(false);
          if (this.pendingRefresh && !this.destroyed) {
            this.pendingRefresh = false;
            this.refresh();
          }
        }),
      )
      .subscribe({
        next: ({ orders, tickets, cash }) => {
          this.orders.set(orders);
          this.tickets.set(tickets);
          this.cash.set(cash);
          this.loaded.set(true);
          this.updatedAt.set(new Date());
        },
        error: () => this.error.set(true),
      });
    this.subscriptions.add(request);
  }

  protected orderLocation(order: RestaurantOrder): string {
    return order.servicePointName || order.customerReference || 'Para llevar';
  }

  protected stockLabel(ingredient: InventoryReportIngredient): string {
    if (ingredient.outOfStock) return 'Agotado';
    if (ingredient.lowStock) return 'Reponer';
    return 'Sin costo';
  }

  protected reservationTime(reservation: Reservation): string {
    return new Intl.DateTimeFormat('es-MX', {
      timeZone: this.branch()?.timezone ?? 'UTC',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(reservation.startAt));
  }

  private refreshAdditional(): void {
    this.refreshInventory();
    this.refreshAgenda();
    this.refreshPromotions();
    this.refreshTips();
  }

  private refreshAgenda(): void {
    const branch = this.branch();
    if (!branch || !this.auth.canOperateCash()) return;
    if (this.agendaLoading()) {
      this.pendingAgendaRefresh = true;
      return;
    }
    this.agendaLoading.set(true);
    this.agendaError.set(false);
    const today = this.dateInZone(new Date(), branch.timezone);
    const request = this.reservationApi
      .findDay(today)
      .pipe(
        finalize(() => {
          this.agendaLoading.set(false);
          if (this.pendingAgendaRefresh && !this.destroyed) {
            this.pendingAgendaRefresh = false;
            this.refreshAgenda();
          }
        }),
      )
      .subscribe({
        next: (reservations) => {
          this.reservations.set(reservations);
          this.agendaLoaded.set(true);
        },
        error: () => this.agendaError.set(true),
      });
    this.subscriptions.add(request);
  }

  private refreshPromotions(): void {
    if (!this.branch() || !this.auth.canViewReports()) return;
    if (this.promotionsLoading()) {
      this.pendingPromotionRefresh = true;
      return;
    }
    this.promotionsLoading.set(true);
    this.promotionsError.set(false);
    const request = this.promotionApi
      .findAll(true)
      .pipe(
        finalize(() => {
          this.promotionsLoading.set(false);
          if (this.pendingPromotionRefresh && !this.destroyed) {
            this.pendingPromotionRefresh = false;
            this.refreshPromotions();
          }
        }),
      )
      .subscribe({
        next: (promotions) => {
          this.promotions.set(promotions);
          this.promotionsLoaded.set(true);
        },
        error: () => this.promotionsError.set(true),
      });
    this.subscriptions.add(request);
  }

  private refreshTips(): void {
    const branch = this.branch();
    if (!branch || !this.auth.canViewReports()) return;
    if (this.tipsLoading()) {
      this.pendingTipsRefresh = true;
      return;
    }
    this.tipsLoading.set(true);
    this.tipsError.set(false);
    const today = this.dateInZone(new Date(), branch.timezone);
    const request = this.reportApi
      .findDaily(branch.id, today)
      .pipe(
        finalize(() => {
          this.tipsLoading.set(false);
          if (this.pendingTipsRefresh && !this.destroyed) {
            this.pendingTipsRefresh = false;
            this.refreshTips();
          }
        }),
      )
      .subscribe({
        next: (report) => this.dailyReport.set(report),
        error: () => this.tipsError.set(true),
      });
    this.subscriptions.add(request);
  }

  private refreshInventory(): void {
    const branch = this.branch();
    if (!branch || !this.auth.canAdminister()) return;
    if (this.inventoryLoading()) {
      this.pendingInventoryRefresh = true;
      return;
    }
    this.inventoryLoading.set(true);
    this.inventoryError.set(false);
    const today = this.dateInZone(new Date(), branch.timezone);
    const request = this.inventoryApi
      .report(today, today)
      .pipe(
        finalize(() => {
          this.inventoryLoading.set(false);
          if (this.pendingInventoryRefresh && !this.destroyed) {
            this.pendingInventoryRefresh = false;
            this.refreshInventory();
          }
        }),
      )
      .subscribe({
        next: (report) => {
          this.inventoryReport.set(report);
          this.inventoryUpdatedAt.set(new Date());
        },
        error: () => this.inventoryError.set(true),
      });
    this.subscriptions.add(request);
  }

  private dateInZone(date: Date, zone: string): string {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: zone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(date);
    const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
    return `${get('year')}-${get('month')}-${get('day')}`;
  }

  private loadBranch(): void {
    if (this.refreshing()) {
      return;
    }
    this.loading.set(true);
    this.error.set(false);
    this.refreshing.set(true);
    const request = this.orderApi.findBranches().subscribe({
      next: (branches) => {
        this.refreshing.set(false);
        const branch = branches[0] ?? null;
        this.branch.set(branch);
        if (!branch) {
          this.loading.set(false);
          return;
        }
        this.branchWatch?.unsubscribe();
        this.branchWatch = this.realtime
          .watchBranch(branch.id)
          .pipe(auditTime(200))
          .subscribe(() => this.refresh());
        this.refresh();
      },
      error: () => {
        this.refreshing.set(false);
        this.error.set(true);
        this.loading.set(false);
      },
    });
    this.subscriptions.add(request);
  }
}
