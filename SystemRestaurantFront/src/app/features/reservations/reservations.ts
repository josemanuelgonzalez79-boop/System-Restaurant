import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { auditTime, finalize, forkJoin, interval, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import { OrderOperator } from '../../core/models/order.model';
import {
  Reservation,
  ReservationEvent,
  ReservationStatus,
} from '../../core/models/reservation.model';
import { Branch, OperationalArea, ServicePoint } from '../../core/models/structure.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { ReservationApiService } from '../../core/services/reservation-api.service';
import { StructureApiService } from '../../core/services/structure-api.service';

@Component({
  selector: 'app-reservations',
  imports: [
    ButtonModule,
    ConfirmDialogModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    SelectModule,
    TagModule,
    TextareaModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './reservations.html',
  styleUrl: './reservations.scss',
})
export class ReservationsPage implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(ReservationApiService);
  private readonly ordersApi = inject(OrderApiService);
  private readonly structure = inject(StructureApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthApiService);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);
  private readonly router = inject(Router);
  private lookup?: Subscription;
  private realtimeSubscription?: Subscription;
  private fallbackSubscription?: Subscription;
  private generation = 0;

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly seatingId = signal<number | null>(null);
  protected readonly dialogVisible = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly branch = signal<Branch | null>(null);
  protected readonly areas = signal<OperationalArea[]>([]);
  protected readonly points = signal<ServicePoint[]>([]);
  protected readonly operators = signal<OrderOperator[]>([]);
  protected readonly reservations = signal<Reservation[]>([]);
  protected readonly selected = signal<Reservation | null>(null);
  protected readonly history = signal<ReservationEvent[]>([]);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<ReservationStatus | 'ALL'>('ALL');
  protected readonly visible = computed(() => {
    const term = this.search().trim().toLocaleLowerCase('es');
    return this.reservations().filter(
      (item) =>
        (this.statusFilter() === 'ALL' || item.status === this.statusFilter()) &&
        (!term ||
          `${item.guestName} ${item.guestPhone ?? ''} ${item.servicePointName}`
            .toLocaleLowerCase('es')
            .includes(term)),
    );
  });
  protected readonly booked = computed(
    () => this.reservations().filter((item) => item.status === 'BOOKED').length,
  );
  protected readonly seated = computed(
    () => this.reservations().filter((item) => item.status === 'SEATED').length,
  );
  protected readonly availablePoints = computed(() => {
    const activeAreas = new Set(
      this.areas()
        .filter((area) => area.active)
        .map((area) => area.id),
    );
    return this.points().filter(
      (point) =>
        point.active &&
        activeAreas.has(point.areaId) &&
        (point.pointType === 'TABLE' || point.pointType === 'ROOM'),
    );
  });

  protected readonly form = this.formBuilder.group({
    servicePointId: this.formBuilder.control<number | null>(null, Validators.required),
    startLocal: this.formBuilder.nonNullable.control('', Validators.required),
    durationMinutes: this.formBuilder.nonNullable.control(90, Validators.required),
    guestName: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(120),
    ]),
    guestPhone: this.formBuilder.nonNullable.control('', Validators.maxLength(30)),
    guestCount: this.formBuilder.nonNullable.control(2, [
      Validators.required,
      Validators.min(1),
      Validators.max(99),
    ]),
    notes: this.formBuilder.nonNullable.control('', Validators.maxLength(500)),
  });
  protected readonly durations = [30, 45, 60, 90, 120, 150, 180, 240].map((minutes) => ({
    value: minutes,
    label: `${minutes} min`,
  }));
  protected readonly statuses: { value: ReservationStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'Todas' },
    { value: 'BOOKED', label: 'Pendientes' },
    { value: 'SEATED', label: 'Sentadas' },
    { value: 'CANCELLED', label: 'Canceladas' },
    { value: 'NO_SHOW', label: 'Ausencias' },
  ];
  protected selectedDate = '';
  protected maximumDate = '';
  protected assignedUserId: number | null = null;
  protected cancelReason = '';
  private editingVersion: number | null = null;

  ngOnInit(): void {
    this.lookup = forkJoin({
      branches: this.structure.findBranches(),
      assigned: this.ordersApi.findBranches(),
      areas: this.structure.findAreas(),
      points: this.structure.findPoints(),
    }).subscribe({
      next: ({ branches, assigned, areas, points }) => {
        const branch = branches[0] ?? null;
        this.branch.set(branch);
        this.areas.set(areas);
        this.points.set(points);
        if (branch) {
          this.selectedDate = this.dateInZone(new Date(), branch.timezone);
          this.maximumDate = this.dateInZone(new Date(Date.now() + 90 * 86400000), branch.timezone);
          this.loadDay();
          this.ordersApi.findOperators(branch.id).subscribe({
            next: (people) => {
              this.operators.set(people);
              this.assignedUserId =
                people.find((person) => person.id === this.auth.user()?.id)?.id ??
                people[0]?.id ??
                null;
            },
            error: () => {},
          });
          if (assigned.some((item) => item.id === branch.id)) {
            this.realtimeSubscription = this.realtime
              .watchBranch(branch.id)
              .pipe(auditTime(300))
              .subscribe(() => this.loadDay());
          }
        } else this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
    this.fallbackSubscription = interval(60_000).subscribe(() => {
      if (!this.saving() && !this.seatingId()) this.loadDay();
    });
  }

  ngOnDestroy(): void {
    this.lookup?.unsubscribe();
    this.realtimeSubscription?.unsubscribe();
    this.fallbackSubscription?.unsubscribe();
  }

  protected selectDate(value: string): void {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return;
    this.selectedDate = value;
    this.selected.set(null);
    this.history.set([]);
    this.loadDay();
  }

  protected loadDay(): void {
    if (!this.branch() || !this.selectedDate) return;
    const generation = ++this.generation;
    this.loading.set(true);
    this.api
      .findDay(this.selectedDate)
      .pipe(
        finalize(() => {
          if (generation === this.generation) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (items) => {
          if (generation !== this.generation) return;
          this.reservations.set(items);
          const selected = items.find((item) => item.id === this.selected()?.id);
          this.selected.set(selected ?? null);
        },
        error: () => {},
      });
  }

  protected newReservation(): void {
    this.editingId.set(null);
    this.editingVersion = null;
    this.form.reset({
      servicePointId: this.availablePoints()[0]?.id ?? null,
      startLocal: this.defaultStart(),
      durationMinutes: 90,
      guestName: '',
      guestPhone: '',
      guestCount: 2,
      notes: '',
    });
    this.dialogVisible.set(true);
  }

  protected edit(item: Reservation): void {
    if (item.status !== 'BOOKED') return;
    this.editingId.set(item.id);
    this.editingVersion = item.version;
    this.form.reset({
      servicePointId: item.servicePointId,
      startLocal: this.localDateTime(item.startAt),
      durationMinutes: Math.round((Date.parse(item.endAt) - Date.parse(item.startAt)) / 60000),
      guestName: item.guestName,
      guestPhone: item.guestPhone ?? '',
      guestCount: item.guestCount,
      notes: item.notes ?? '',
    });
    this.dialogVisible.set(true);
  }

  protected save(): void {
    if (this.form.invalid || !this.form.controls.guestName.value.trim() || this.saving()) {
      this.form.markAllAsTouched();
      this.messages.add({ severity: 'warn', summary: 'Completa los datos de la reservación' });
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      servicePointId: value.servicePointId!,
      startLocal: value.startLocal,
      durationMinutes: value.durationMinutes,
      guestName: value.guestName.trim(),
      guestPhone: value.guestPhone.trim(),
      guestCount: value.guestCount,
      notes: value.notes.trim(),
      ...(this.editingId() !== null ? { expectedVersion: this.editingVersion! } : {}),
    };
    this.saving.set(true);
    const request =
      this.editingId() === null
        ? this.api.create(payload)
        : this.api.update(this.editingId()!, payload);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (item) => {
        this.dialogVisible.set(false);
        this.selectedDate = this.dateInZone(new Date(item.startAt), item.timezone);
        this.selected.set(item);
        this.loadHistory(item.id);
        this.loadDay();
        this.messages.add({ severity: 'success', summary: `Reservación #${item.id} guardada` });
      },
      error: () => {},
    });
  }

  protected select(item: Reservation): void {
    this.selected.set(item);
    this.cancelReason = '';
    this.loadHistory(item.id);
  }

  protected cancel(item: Reservation): void {
    if (this.cancelReason.trim().length < 5) {
      this.messages.add({
        severity: 'warn',
        summary: 'Escribe un motivo de al menos 5 caracteres',
      });
      return;
    }
    this.saving.set(true);
    this.api
      .cancel(item.id, item.version, this.cancelReason.trim())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => this.afterAction(updated, 'Reservación cancelada'),
        error: () => {},
      });
  }

  protected noShow(item: Reservation): void {
    this.confirmations.confirm({
      header: 'Registrar ausencia',
      message: `¿Confirmas que ${item.guestName} no se presentó? La mesa quedará disponible.`,
      icon: 'pi pi-clock',
      rejectLabel: 'Conservar',
      acceptLabel: 'Registrar ausencia',
      accept: () => {
        this.saving.set(true);
        this.api
          .noShow(item.id, item.version)
          .pipe(finalize(() => this.saving.set(false)))
          .subscribe({
            next: (updated) => this.afterAction(updated, 'Ausencia registrada'),
            error: () => {},
          });
      },
    });
  }

  protected canSeat(item: Reservation): boolean {
    const now = Date.now();
    return (
      item.status === 'BOOKED' &&
      now >= Date.parse(item.startAt) - 30 * 60000 &&
      now < Date.parse(item.endAt)
    );
  }

  protected canNoShow(item: Reservation): boolean {
    return item.status === 'BOOKED' && Date.now() >= Date.parse(item.startAt) + 15 * 60000;
  }

  protected seat(item: Reservation): void {
    const branch = this.branch();
    if (!branch || this.assignedUserId === null || !this.canSeat(item) || this.seatingId()) return;
    this.seatingId.set(item.id);
    this.ordersApi
      .create({
        branchId: branch.id,
        servicePointId: item.servicePointId,
        serviceMode: 'DINE_IN',
        assignedUserId: this.assignedUserId,
        guestCount: item.guestCount,
        customerReference: item.guestName,
        notes: item.notes ?? '',
        reservationId: item.id,
      })
      .pipe(finalize(() => this.seatingId.set(null)))
      .subscribe({
        next: (order) => {
          this.messages.add({
            severity: 'success',
            summary: `${order.folio} abierto desde la reservación`,
          });
          void this.router.navigate(['/pedidos', order.id]);
        },
        error: () => this.loadDay(),
      });
  }

  protected statusLabel(status: ReservationStatus): string {
    return {
      BOOKED: 'Pendiente',
      SEATED: 'Sentada',
      CANCELLED: 'Cancelada',
      NO_SHOW: 'No asistió',
    }[status];
  }

  protected actionLabel(action: ReservationEvent['action']): string {
    return {
      CREATED: 'Creada',
      RESCHEDULED: 'Actualizada',
      CANCELLED: 'Cancelada',
      NO_SHOW: 'Ausencia',
      SEATED: 'Cliente sentado',
    }[action];
  }

  protected statusSeverity(status: ReservationStatus): 'info' | 'success' | 'danger' | 'warn' {
    const severities: Record<ReservationStatus, 'info' | 'success' | 'danger' | 'warn'> = {
      BOOKED: 'info',
      SEATED: 'success',
      CANCELLED: 'danger',
      NO_SHOW: 'warn',
    };
    return severities[status];
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('es-MX', {
      timeZone: this.branch()?.timezone ?? 'UTC',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).format(new Date(value));
  }

  protected formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('es-MX', {
      timeZone: this.branch()?.timezone ?? 'UTC',
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  private loadHistory(id: number): void {
    this.history.set([]);
    this.api.history(id).subscribe({
      next: (events) => {
        if (this.selected()?.id === id) this.history.set(events);
      },
      error: () => {},
    });
  }

  private afterAction(item: Reservation, summary: string): void {
    this.selected.set(item);
    this.loadHistory(item.id);
    this.loadDay();
    this.messages.add({ severity: 'success', summary });
  }

  private defaultStart(): string {
    const zone = this.branch()?.timezone ?? 'UTC';
    const proposed = new Date(Date.now() + 45 * 60000);
    const date = this.selectedDate || this.dateInZone(proposed, zone);
    if (date > this.dateInZone(new Date(), zone)) return `${date}T19:00`;
    const local = this.localDateTime(proposed);
    const minutes = Number(local.slice(-2));
    const rounded = Math.ceil(minutes / 15) * 15;
    const hour = Number(local.slice(11, 13));
    if (rounded === 60) {
      const next = new Date(proposed.getTime() + (60 - minutes) * 60000);
      return this.localDateTime(next).slice(0, 16);
    }
    return `${local.slice(0, 11)}${String(hour).padStart(2, '0')}:${String(rounded).padStart(2, '0')}`;
  }

  private localDateTime(value: string | Date): string {
    const date = typeof value === 'string' ? new Date(value) : value;
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: this.branch()?.timezone ?? 'UTC',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(date);
    const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
    return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}`;
  }

  private dateInZone(value: Date, zone: string): string {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: zone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(value);
    const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
    return `${get('year')}-${get('month')}-${get('day')}`;
  }
}
