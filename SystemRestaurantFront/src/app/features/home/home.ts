import { CurrencyPipe, DatePipe, DecimalPipe, registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { auditTime, finalize, forkJoin, interval, of, Subscription } from 'rxjs';

import { CashRegisterSummary } from '../../core/models/cash-register.model';
import {
  InventoryReport,
  InventoryReportIngredient,
} from '../../core/models/inventory-report.model';
import { RestaurantOrder } from '../../core/models/order.model';
import { PreparationTicket } from '../../core/models/preparation.model';
import { Branch } from '../../core/models/structure.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { CashRegisterApiService } from '../../core/services/cash-register-api.service';
import { InventoryReportApiService } from '../../core/services/inventory-report-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { PreparationApiService } from '../../core/services/preparation-api.service';
import { RealtimeService } from '../../core/services/realtime.service';

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
  private readonly realtime = inject(RealtimeService);
  protected readonly auth = inject(AuthApiService);

  private readonly subscriptions = new Subscription();
  private branchWatch?: Subscription;
  private pendingRefresh = false;
  private pendingInventoryRefresh = false;
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
      this.refreshInventory();
      return;
    }
    this.refreshInventory();
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
