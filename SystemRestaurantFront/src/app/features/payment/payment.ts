import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { auditTime, filter, finalize, Subscription } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import { OrderPayment, PaymentMethod, PaymentSummary } from '../../core/models/payment.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { PaymentApiService } from '../../core/services/payment-api.service';
import { RealtimeService } from '../../core/services/realtime.service';

@Component({
  selector: 'app-payment',
  imports: [
    ButtonModule,
    ConfirmDialogModule,
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
  providers: [ConfirmationService],
  templateUrl: './payment.html',
  styleUrl: './payment.scss',
})
export class PaymentPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly paymentApi = inject(PaymentApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly auth = inject(AuthApiService);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);
  private realtimeSubscription?: Subscription;

  private readonly orderId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly saving = signal(false);
  protected readonly closing = signal(false);
  protected readonly voiding = signal(false);
  protected readonly summary = signal<PaymentSummary | null>(null);
  protected readonly selectedMethod = signal<PaymentMethod>('CASH');
  protected readonly enteredAmount = signal(0);
  protected readonly tenderedAmount = signal(0);
  protected readonly voidDialogVisible = signal(false);
  protected readonly selectedPayment = signal<OrderPayment | null>(null);

  protected readonly methodOptions: Array<{
    value: PaymentMethod;
    label: string;
    icon: string;
  }> = [
    { value: 'CASH', label: 'Efectivo', icon: 'pi pi-money-bill' },
    { value: 'CARD', label: 'Tarjeta', icon: 'pi pi-credit-card' },
    { value: 'TRANSFER', label: 'Transferencia', icon: 'pi pi-arrow-right-arrow-left' },
    { value: 'OTHER', label: 'Otro', icon: 'pi pi-wallet' },
  ];

  protected readonly canVoid = computed(() => {
    const role = this.auth.user()?.role;
    return role === 'OWNER' || role === 'ADMIN' || role === 'MANAGER' || role === 'CASHIER';
  });
  protected readonly cashChange = computed(() =>
    this.selectedMethod() === 'CASH'
      ? Math.max(0, this.tenderedAmount() - this.enteredAmount())
      : 0,
  );
  protected readonly orderActive = computed(() => {
    const status = this.summary()?.orderDetail.order.status;
    return status === 'OPEN' || status === 'IN_PROGRESS';
  });

  protected readonly paymentForm = this.formBuilder.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01)]],
    tenderedAmount: [0, [Validators.required, Validators.min(0.01)]],
    method: ['CASH' as PaymentMethod, Validators.required],
    methodLabel: ['', Validators.maxLength(60)],
    reference: ['', Validators.maxLength(120)],
    notes: ['', Validators.maxLength(250)],
  });

  protected readonly voidForm = this.formBuilder.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(250)]],
  });

  ngOnInit(): void {
    if (!Number.isInteger(this.orderId) || this.orderId <= 0) {
      void this.router.navigate(['/pedidos']);
      return;
    }
    this.bindFormSignals();
    this.load(false);
  }

  ngOnDestroy(): void {
    this.realtimeSubscription?.unsubscribe();
  }

  protected applyBalance(): void {
    const balance = this.summary()?.balance ?? 0;
    this.paymentForm.controls.amount.setValue(balance);
    this.paymentForm.controls.tenderedAmount.setValue(balance);
  }

  protected submitPayment(): void {
    const current = this.summary();
    if (!current || this.paymentForm.invalid || !this.orderActive() || current.settled) {
      this.paymentForm.markAllAsTouched();
      return;
    }
    const value = this.paymentForm.getRawValue();
    if (value.amount > current.balance) {
      this.messages.add({
        severity: 'warn',
        summary: 'Importe mayor al saldo',
        detail: 'El importe aplicado no puede superar el saldo pendiente.',
      });
      return;
    }
    if (value.method === 'OTHER' && !value.methodLabel.trim()) {
      this.messages.add({
        severity: 'warn',
        summary: 'Describe la forma de pago',
        detail: 'Escribe el nombre de la otra forma de pago.',
      });
      return;
    }
    if (value.method === 'CASH' && value.tenderedAmount < value.amount) {
      this.messages.add({
        severity: 'warn',
        summary: 'Efectivo insuficiente',
        detail: 'El efectivo recibido debe cubrir el importe aplicado.',
      });
      return;
    }

    this.saving.set(true);
    this.paymentApi
      .add(this.orderId, {
        operationId: this.newOperationId(),
        amount: value.amount,
        tenderedAmount: value.method === 'CASH' ? value.tenderedAmount : value.amount,
        method: value.method,
        methodLabel: value.methodLabel,
        reference: value.reference,
        notes: value.notes,
        orderVersion: current.orderDetail.order.version,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((updated) => {
        this.setSummary(updated);
        this.messages.add({
          severity: 'success',
          summary: 'Cobro registrado',
          detail:
            value.method === 'CASH' && value.tenderedAmount > value.amount
              ? `Entrega de cambio: ${this.currency(value.tenderedAmount - value.amount)}.`
              : 'El saldo del pedido fue actualizado.',
        });
      });
  }

  protected openVoid(payment: OrderPayment): void {
    this.selectedPayment.set(payment);
    this.voidForm.reset({ reason: '' });
    this.voidDialogVisible.set(true);
  }

  protected closeVoidDialog(): void {
    this.voidDialogVisible.set(false);
    this.selectedPayment.set(null);
  }

  protected submitVoid(): void {
    const current = this.summary();
    const payment = this.selectedPayment();
    if (!current || !payment || this.voidForm.invalid) {
      this.voidForm.markAllAsTouched();
      return;
    }
    this.voiding.set(true);
    this.paymentApi
      .voidPayment(
        this.orderId,
        payment.id,
        current.orderDetail.order.version,
        this.voidForm.getRawValue().reason,
      )
      .pipe(finalize(() => this.voiding.set(false)))
      .subscribe((updated) => {
        this.setSummary(updated);
        this.closeVoidDialog();
        this.messages.add({
          severity: 'warn',
          summary: 'Cobro anulado',
          detail: `${payment.folio} permanece en el historial para auditoría.`,
        });
      });
  }

  protected closeOrder(): void {
    const current = this.summary();
    if (!current?.canClose) {
      return;
    }
    this.confirmations.confirm({
      header: 'Cerrar pedido pagado',
      message: `¿Confirmas el cierre de ${current.orderDetail.order.folio}? La mesa quedará libre.`,
      icon: 'pi pi-check-circle',
      rejectLabel: 'Conservar abierto',
      acceptLabel: 'Cerrar pedido',
      accept: () => {
        this.closing.set(true);
        this.paymentApi
          .close(this.orderId, current.orderDetail.order.version)
          .pipe(finalize(() => this.closing.set(false)))
          .subscribe(() => {
            this.messages.add({
              severity: 'success',
              summary: 'Pedido cerrado',
              detail: 'El cobro quedó completo y la mesa fue liberada.',
            });
            void this.router.navigate(['/pedidos']);
          });
      },
    });
  }

  protected paymentMethodLabel(payment: OrderPayment): string {
    if (payment.method === 'OTHER') {
      return payment.methodLabel ?? 'Otro';
    }
    return (
      this.methodOptions.find((method) => method.value === payment.method)?.label ?? payment.method
    );
  }

  protected printReceipt(): void {
    window.print();
  }

  protected backToOrder(): void {
    void this.router.navigate(['/pedidos', this.orderId]);
  }

  protected refresh(): void {
    this.load(false);
  }

  private bindFormSignals(): void {
    this.paymentForm.controls.method.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((method) => {
        this.selectedMethod.set(method);
        if (method !== 'CASH') {
          this.paymentForm.controls.tenderedAmount.setValue(this.paymentForm.controls.amount.value);
        }
      });
    this.paymentForm.controls.amount.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((amount) => {
        this.enteredAmount.set(amount);
        if (this.paymentForm.controls.method.value !== 'CASH') {
          this.paymentForm.controls.tenderedAmount.setValue(amount, { emitEvent: false });
          this.tenderedAmount.set(amount);
        }
      });
    this.paymentForm.controls.tenderedAmount.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((amount) => this.tenderedAmount.set(amount));
  }

  private load(silent: boolean): void {
    if (!silent) {
      this.refreshing.set(true);
    }
    this.paymentApi
      .findSummary(this.orderId)
      .pipe(
        finalize(() => {
          this.loading.set(false);
          if (!silent) {
            this.refreshing.set(false);
          }
        }),
      )
      .subscribe((summary) => {
        this.setSummary(summary, !silent);
        this.watchOrder(summary.orderDetail.order.branchId);
      });
  }

  private setSummary(summary: PaymentSummary, resetForm = true): void {
    this.summary.set(summary);
    if (!resetForm) {
      const currentAmount = this.paymentForm.controls.amount.value;
      if (!this.orderActive() || summary.settled || currentAmount > summary.balance) {
        const nextAmount = this.orderActive() && !summary.settled ? summary.balance : 0;
        this.paymentForm.controls.amount.setValue(nextAmount);
        this.paymentForm.controls.tenderedAmount.setValue(nextAmount);
      }
      return;
    }
    const amount = summary.settled || !this.orderActive() ? 0 : summary.balance;
    this.paymentForm.reset({
      amount,
      tenderedAmount: amount,
      method: 'CASH',
      methodLabel: '',
      reference: '',
      notes: '',
    });
    this.selectedMethod.set('CASH');
    this.enteredAmount.set(amount);
    this.tenderedAmount.set(amount);
  }

  private watchOrder(branchId: number): void {
    if (this.realtimeSubscription) {
      return;
    }
    this.realtimeSubscription = this.realtime
      .watchBranch(branchId)
      .pipe(
        filter((event) => event.orderId === this.orderId),
        auditTime(200),
      )
      .subscribe(() => this.load(true));
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
