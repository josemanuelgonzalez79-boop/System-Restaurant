import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { auditTime, finalize, forkJoin, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import {
  CashMovement,
  CashMovementType,
  CashRegisterSummary,
} from '../../core/models/cash-register.model';
import { Branch } from '../../core/models/structure.model';
import { CashRegisterApiService } from '../../core/services/cash-register-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';

@Component({
  selector: 'app-cash-register',
  imports: [
    ButtonModule,
    CurrencyPipe,
    DatePipe,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    SelectModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './cash-register.html',
  styleUrl: './cash-register.scss',
})
export class CashRegisterPage implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly orderApi = inject(OrderApiService);
  private readonly cashApi = inject(CashRegisterApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly messages = inject(MessageService);
  private realtimeSubscription?: Subscription;
  private requestActive = false;
  private pendingRealtimeRefresh = false;

  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly saving = signal(false);
  protected readonly branches = signal<Branch[]>([]);
  protected readonly selectedBranchId = signal<number | null>(null);
  protected readonly current = signal<CashRegisterSummary | null>(null);
  protected readonly history = signal<CashRegisterSummary[]>([]);
  protected readonly showHistory = signal(false);
  protected readonly movementDialogVisible = signal(false);
  protected readonly closeDialogVisible = signal(false);
  protected readonly voidDialogVisible = signal(false);
  protected readonly selectedMovement = signal<CashMovement | null>(null);

  protected readonly electronicPayments = computed(() => {
    const summary = this.current();
    return summary ? summary.cardPayments + summary.transferPayments + summary.otherPayments : 0;
  });
  protected readonly netManualMovements = computed(() => {
    const summary = this.current();
    return summary ? summary.cashIn - summary.cashOut : 0;
  });
  protected readonly countedDifference = computed(() => {
    const summary = this.current();
    if (!summary) {
      return 0;
    }
    return this.closeForm.controls.countedCash.value - summary.expectedCash;
  });

  protected readonly movementTypes: Array<{ value: CashMovementType; label: string }> = [
    { value: 'CASH_IN', label: 'Entrada de efectivo' },
    { value: 'CASH_OUT', label: 'Salida de efectivo' },
  ];

  protected readonly openForm = this.formBuilder.nonNullable.group({
    openingAmount: [0, [Validators.required, Validators.min(0)]],
    notes: ['', Validators.maxLength(250)],
  });

  protected readonly movementForm = this.formBuilder.nonNullable.group({
    movementType: ['CASH_IN' as CashMovementType, Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    concept: ['', [Validators.required, Validators.maxLength(160)]],
  });

  protected readonly closeForm = this.formBuilder.nonNullable.group({
    countedCash: [0, [Validators.required, Validators.min(0)]],
    notes: ['', Validators.maxLength(250)],
  });

  protected readonly voidForm = this.formBuilder.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(250)]],
  });

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

  protected changeBranch(branchId: number | null): void {
    this.realtimeSubscription?.unsubscribe();
    this.realtimeSubscription = undefined;
    this.pendingRealtimeRefresh = false;
    this.selectedBranchId.set(branchId);
    this.current.set(null);
    this.history.set([]);
    if (branchId !== null) {
      this.watchBranch(branchId);
      this.loadCash(false);
    }
  }

  protected selectBranch(value: string): void {
    const branchId = Number(value);
    this.changeBranch(Number.isInteger(branchId) && branchId > 0 ? branchId : null);
  }

  protected refresh(): void {
    this.loadCash(false);
  }

  protected toggleHistory(): void {
    this.showHistory.update((visible) => !visible);
  }

  protected submitOpen(): void {
    const branchId = this.selectedBranchId();
    if (branchId === null || this.openForm.invalid) {
      this.openForm.markAllAsTouched();
      return;
    }
    const value = this.openForm.getRawValue();
    this.saving.set(true);
    this.cashApi
      .open({ branchId, openingAmount: value.openingAmount, notes: value.notes })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((summary) => {
        this.current.set(summary);
        this.upsertHistory(summary);
        this.openForm.reset({ openingAmount: 0, notes: '' });
        this.messages.add({
          severity: 'success',
          summary: 'Caja abierta',
          detail: `${summary.session.folio} está lista para recibir cobros.`,
        });
      });
  }

  protected openMovement(type: CashMovementType): void {
    this.movementForm.reset({ movementType: type, amount: 0, concept: '' });
    this.movementDialogVisible.set(true);
  }

  protected closeMovementDialog(): void {
    this.movementDialogVisible.set(false);
  }

  protected submitMovement(): void {
    const summary = this.current();
    if (!summary || this.movementForm.invalid) {
      this.movementForm.markAllAsTouched();
      return;
    }
    const value = this.movementForm.getRawValue();
    this.saving.set(true);
    this.cashApi
      .addMovement(summary.session.id, {
        operationId: this.newOperationId(),
        movementType: value.movementType,
        amount: value.amount,
        concept: value.concept,
        sessionVersion: summary.session.version,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((updated) => {
        this.current.set(updated);
        this.upsertHistory(updated);
        this.closeMovementDialog();
        this.messages.add({
          severity: 'success',
          summary: value.movementType === 'CASH_IN' ? 'Entrada registrada' : 'Salida registrada',
          detail: 'El efectivo esperado de la caja fue actualizado.',
        });
      });
  }

  protected openVoid(movement: CashMovement): void {
    this.selectedMovement.set(movement);
    this.voidForm.reset({ reason: '' });
    this.voidDialogVisible.set(true);
  }

  protected closeVoidDialog(): void {
    this.voidDialogVisible.set(false);
    this.selectedMovement.set(null);
  }

  protected submitVoid(): void {
    const summary = this.current();
    const movement = this.selectedMovement();
    if (!summary || !movement || this.voidForm.invalid) {
      this.voidForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.cashApi
      .voidMovement(
        summary.session.id,
        movement.id,
        summary.session.version,
        this.voidForm.getRawValue().reason,
      )
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((updated) => {
        this.current.set(updated);
        this.upsertHistory(updated);
        this.closeVoidDialog();
        this.messages.add({
          severity: 'warn',
          summary: 'Movimiento anulado',
          detail: `${movement.folio} permanece en el historial de caja.`,
        });
      });
  }

  protected openCloseDialog(): void {
    const summary = this.current();
    if (!summary?.canClose) {
      return;
    }
    this.closeForm.reset({ countedCash: summary.expectedCash, notes: '' });
    this.closeDialogVisible.set(true);
  }

  protected closeCloseDialog(): void {
    this.closeDialogVisible.set(false);
  }

  protected submitClose(): void {
    const summary = this.current();
    if (!summary || this.closeForm.invalid) {
      this.closeForm.markAllAsTouched();
      return;
    }
    const value = this.closeForm.getRawValue();
    this.saving.set(true);
    this.cashApi
      .close(summary.session.id, summary.session.version, value.countedCash, value.notes)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((closed) => {
        this.upsertHistory(closed);
        this.current.set(null);
        this.closeCloseDialog();
        this.messages.add({
          severity: closed.session.differenceAtClose === 0 ? 'success' : 'warn',
          summary: 'Caja cerrada',
          detail:
            closed.session.differenceAtClose === 0
              ? 'El conteo coincide con el efectivo esperado.'
              : `El corte registró una diferencia de ${this.currency(
                  closed.session.differenceAtClose ?? 0,
                )}.`,
        });
      });
  }

  protected movementLabel(type: CashMovementType): string {
    return type === 'CASH_IN' ? 'Entrada' : 'Salida';
  }

  protected printCut(): void {
    window.print();
  }

  private loadCash(silent: boolean): void {
    const branchId = this.selectedBranchId();
    if (branchId === null) {
      return;
    }
    if (this.requestActive) {
      this.pendingRealtimeRefresh = true;
      return;
    }
    this.requestActive = true;
    if (!silent) {
      this.refreshing.set(true);
    }
    forkJoin({
      current: this.cashApi.findCurrent(branchId),
      history: this.cashApi.findHistory(branchId),
    })
      .pipe(
        finalize(() => {
          this.requestActive = false;
          this.loading.set(false);
          if (!silent) {
            this.refreshing.set(false);
          }
          if (this.pendingRealtimeRefresh && this.selectedBranchId() === branchId) {
            this.pendingRealtimeRefresh = false;
            this.loadCash(true);
          }
        }),
      )
      .subscribe(({ current, history }) => {
        if (this.selectedBranchId() !== branchId) {
          return;
        }
        this.current.set(current);
        this.history.set(history);
      });
  }

  private watchBranch(branchId: number): void {
    this.realtimeSubscription = this.realtime
      .watchBranch(branchId)
      .pipe(auditTime(250))
      .subscribe(() => {
        if (this.selectedBranchId() === branchId) {
          this.loadCash(true);
        }
      });
  }

  private upsertHistory(summary: CashRegisterSummary): void {
    this.history.update((items) => [
      summary,
      ...items.filter((item) => item.session.id !== summary.session.id),
    ]);
  }

  private currency(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
  }

  private newOperationId(): string {
    if (typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = [...bytes].map((value) => value.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  }
}
