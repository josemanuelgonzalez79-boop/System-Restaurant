import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { auditTime, finalize, forkJoin, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import {
  OrderCreatePayload,
  OrderOperator,
  OrderStatus,
  RestaurantOrder,
} from '../../core/models/order.model';
import { Branch, OperationalArea, ServicePoint } from '../../core/models/structure.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { StructureApiService } from '../../core/services/structure-api.service';

@Component({
  selector: 'app-orders',
  imports: [
    ButtonModule,
    ConfirmDialogModule,
    DatePipe,
    DialogModule,
    FormsModule,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    SelectModule,
    TagModule,
    TextareaModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class Orders implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly orderApi = inject(OrderApiService);
  private readonly structureApi = inject(StructureApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthApiService);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);
  private readonly router = inject(Router);
  private realtimeSubscription?: Subscription;
  private pendingRealtimeRefresh = false;

  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly saving = signal(false);
  protected readonly changingStatusId = signal<number | null>(null);
  protected readonly dialogVisible = signal(false);

  protected readonly branches = signal<Branch[]>([]);
  protected readonly areas = signal<OperationalArea[]>([]);
  protected readonly points = signal<ServicePoint[]>([]);
  protected readonly orders = signal<RestaurantOrder[]>([]);
  protected readonly operators = signal<OrderOperator[]>([]);

  protected readonly selectedBranchId = signal<number | null>(null);
  protected readonly selectedAreaId = signal<number | null>(null);
  protected readonly selectedPoint = signal<ServicePoint | null>(null);

  protected readonly selectedBranch = computed(() =>
    this.branches().find((branch) => branch.id === this.selectedBranchId()),
  );
  protected readonly orderableAreas = computed(() => {
    const branchId = this.selectedBranchId();
    return this.areas().filter(
      (area) =>
        area.branchId === branchId &&
        area.active &&
        ['SERVICE', 'CHECKOUT', 'OTHER'].includes(area.areaType),
    );
  });
  protected readonly visiblePoints = computed(() => {
    const branchId = this.selectedBranchId();
    const areaId = this.selectedAreaId();
    const orderableAreaIds = new Set(this.orderableAreas().map((area) => area.id));
    return this.points().filter(
      (point) =>
        point.branchId === branchId &&
        point.active &&
        orderableAreaIds.has(point.areaId) &&
        (areaId === null || point.areaId === areaId),
    );
  });
  protected readonly activeOrdersByPoint = computed(() => {
    const result = new Map<number, RestaurantOrder>();
    this.orders().forEach((order) => {
      if (order.servicePointId !== null) {
        result.set(order.servicePointId, order);
      }
    });
    return result;
  });
  protected readonly takeoutOrders = computed(() =>
    this.orders().filter((order) => order.serviceMode === 'TAKEOUT'),
  );
  protected readonly occupiedPointCount = computed(
    () => this.visiblePoints().filter((point) => this.activeOrdersByPoint().has(point.id)).length,
  );
  protected readonly freePointCount = computed(
    () => this.visiblePoints().length - this.occupiedPointCount(),
  );

  protected readonly orderForm = this.formBuilder.group({
    assignedUserId: this.formBuilder.control<number | null>(null, Validators.required),
    guestCount: this.formBuilder.nonNullable.control(1, [
      Validators.required,
      Validators.min(1),
      Validators.max(99),
    ]),
    customerReference: this.formBuilder.nonNullable.control('', Validators.maxLength(120)),
    notes: this.formBuilder.nonNullable.control('', Validators.maxLength(500)),
  });

  ngOnInit(): void {
    this.loadStructure();
  }

  ngOnDestroy(): void {
    this.realtimeSubscription?.unsubscribe();
  }

  protected changeBranch(branchId: number | null): void {
    this.realtimeSubscription?.unsubscribe();
    this.realtimeSubscription = undefined;
    this.pendingRealtimeRefresh = false;
    this.selectedBranchId.set(branchId);
    this.selectedAreaId.set(null);
    this.orders.set([]);
    this.operators.set([]);
    if (branchId !== null) {
      this.watchBranch(branchId);
      this.loadOperation(branchId);
    }
  }

  protected selectArea(areaId: number | null): void {
    this.selectedAreaId.set(areaId);
  }

  protected openPoint(point: ServicePoint): void {
    if (this.activeOrdersByPoint().has(point.id)) {
      return;
    }
    this.openDialog(point);
  }

  protected openTakeout(): void {
    this.openDialog(null);
  }

  protected closeDialog(): void {
    this.dialogVisible.set(false);
    this.selectedPoint.set(null);
    this.orderForm.reset({
      assignedUserId: null,
      guestCount: 1,
      customerReference: '',
      notes: '',
    });
  }

  protected submit(): void {
    const branchId = this.selectedBranchId();
    if (branchId === null || this.orderForm.invalid) {
      this.orderForm.markAllAsTouched();
      this.messages.add({
        severity: 'warn',
        summary: 'Revisa el pedido',
        detail: 'Selecciona un responsable y corrige los campos marcados.',
      });
      return;
    }

    const value = this.orderForm.getRawValue();
    const point = this.selectedPoint();
    const payload: OrderCreatePayload = {
      branchId,
      servicePointId: point?.id ?? null,
      serviceMode: point ? 'DINE_IN' : 'TAKEOUT',
      assignedUserId: value.assignedUserId as number,
      guestCount: value.guestCount,
      customerReference: value.customerReference,
      notes: value.notes,
    };

    this.saving.set(true);
    this.orderApi
      .create(payload)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((order) => {
        this.orders.update((items) => this.upsert(items, order));
        this.closeDialog();
        this.messages.add({
          severity: 'success',
          summary: 'Pedido abierto',
          detail: `${order.folio} quedó listo para capturar productos.`,
        });
        void this.router.navigate(['/pedidos', order.id]);
      });
  }

  protected openCapture(order: RestaurantOrder): void {
    void this.router.navigate(['/pedidos', order.id]);
  }

  protected startOrder(order: RestaurantOrder): void {
    this.performStatusChange(order, 'IN_PROGRESS');
  }

  protected openPayment(order: RestaurantOrder): void {
    void this.router.navigate(['/pedidos', order.id, 'cobro']);
  }

  protected cancelOrder(order: RestaurantOrder): void {
    this.confirmations.confirm({
      header: 'Cancelar pedido',
      message: `¿Deseas cancelar ${order.folio}? La mesa o punto quedará libre.`,
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: 'Conservar pedido',
      acceptLabel: 'Cancelar pedido',
      acceptButtonProps: { severity: 'danger' },
      accept: () => this.performStatusChange(order, 'CANCELLED'),
    });
  }

  protected refresh(): void {
    const branchId = this.selectedBranchId();
    if (branchId !== null) {
      this.loadOperation(branchId);
    }
  }

  protected orderForPoint(pointId: number): RestaurantOrder | undefined {
    return this.activeOrdersByPoint().get(pointId);
  }

  protected statusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      OPEN: 'Abierto',
      IN_PROGRESS: 'En atención',
      COMPLETED: 'Completado',
      CANCELLED: 'Cancelado',
    };
    return labels[status];
  }

  protected statusSeverity(status: OrderStatus): 'info' | 'warn' | 'success' | 'danger' {
    const severities: Record<OrderStatus, 'info' | 'warn' | 'success' | 'danger'> = {
      OPEN: 'info',
      IN_PROGRESS: 'warn',
      COMPLETED: 'success',
      CANCELLED: 'danger',
    };
    return severities[status];
  }

  protected pointIcon(point: ServicePoint): string {
    const icons: Record<string, string> = {
      TABLE: 'pi pi-th-large',
      COUNTER: 'pi pi-shop',
      CHECKOUT: 'pi pi-credit-card',
      WINDOW: 'pi pi-window-maximize',
    };
    return icons[point.pointType] ?? 'pi pi-map-marker';
  }

  private loadStructure(): void {
    this.loading.set(true);
    forkJoin({
      branches: this.orderApi.findBranches(),
      areas: this.structureApi.findAreas(),
      points: this.structureApi.findPoints(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ branches, areas, points }) => {
        this.branches.set(branches);
        this.areas.set(areas);
        this.points.set(points);
        this.changeBranch(branches[0]?.id ?? null);
      });
  }

  private loadOperation(branchId: number): void {
    this.refreshing.set(true);
    forkJoin({
      orders: this.orderApi.findOrders(branchId),
      operators: this.orderApi.findOperators(branchId),
    })
      .pipe(
        finalize(() => {
          this.refreshing.set(false);
          if (this.pendingRealtimeRefresh && this.selectedBranchId() === branchId) {
            this.pendingRealtimeRefresh = false;
            this.loadOperation(branchId);
          }
        }),
      )
      .subscribe(({ orders, operators }) => {
        if (this.selectedBranchId() !== branchId) {
          return;
        }
        this.orders.set(orders);
        this.operators.set(operators);
      });
  }

  private watchBranch(branchId: number): void {
    this.realtimeSubscription = this.realtime
      .watchBranch(branchId)
      .pipe(auditTime(200))
      .subscribe(() => {
        if (this.selectedBranchId() !== branchId) {
          return;
        }
        if (this.refreshing()) {
          this.pendingRealtimeRefresh = true;
          return;
        }
        this.loadOperation(branchId);
      });
  }

  private openDialog(point: ServicePoint | null): void {
    if (this.operators().length === 0) {
      this.messages.add({
        severity: 'warn',
        summary: 'Falta personal asignado',
        detail: 'Asigna al menos un usuario activo a esta sucursal desde Estructura.',
      });
      return;
    }

    const currentUserId = this.auth.user()?.id;
    const defaultOperator =
      this.operators().find((operator) => operator.id === currentUserId) ?? this.operators()[0];
    this.selectedPoint.set(point);
    this.orderForm.reset({
      assignedUserId: defaultOperator.id,
      guestCount: point ? 2 : 1,
      customerReference: '',
      notes: '',
    });
    this.dialogVisible.set(true);
  }

  private performStatusChange(order: RestaurantOrder, status: OrderStatus): void {
    this.changingStatusId.set(order.id);
    this.orderApi
      .changeStatus(order.id, status, order.version)
      .pipe(finalize(() => this.changingStatusId.set(null)))
      .subscribe((updated) => {
        if (updated.status === 'COMPLETED' || updated.status === 'CANCELLED') {
          this.orders.update((items) => items.filter((item) => item.id !== updated.id));
        } else {
          this.orders.update((items) => this.upsert(items, updated));
        }
        this.messages.add({
          severity: 'success',
          summary: this.statusLabel(updated.status),
          detail: `${updated.folio} fue actualizado.`,
        });
      });
  }

  private upsert(items: RestaurantOrder[], updated: RestaurantOrder): RestaurantOrder[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [updated, ...items];
    return result.sort(
      (left, right) => new Date(right.openedAt).getTime() - new Date(left.openedAt).getTime(),
    );
  }
}
