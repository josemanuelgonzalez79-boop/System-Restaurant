import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';

import { DiscountType, Promotion } from '../../core/models/promotion.model';
import { PromotionApiService } from '../../core/services/promotion-api.service';

@Component({
  selector: 'app-promotions',
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
  ],
  templateUrl: './promotions.html',
  styleUrl: './promotions.scss',
})
export class PromotionsPage implements OnInit {
  private readonly api = inject(PromotionApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);

  protected readonly promotions = signal<Promotion[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editing = signal<Promotion | null>(null);
  protected readonly typeOptions = [
    { label: 'Porcentaje', value: 'PERCENT' as DiscountType },
    { label: 'Importe fijo', value: 'FIXED' as DiscountType },
  ];
  protected readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]{3,24}$/)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    discountType: ['PERCENT' as DiscountType, Validators.required],
    discountValue: [10, [Validators.required, Validators.min(0.01)]],
    minimumSubtotal: [0, [Validators.required, Validators.min(0)]],
    startsOn: ['', Validators.required],
    endsOn: ['', Validators.required],
    active: [true],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .findAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (items) => this.promotions.set(items),
        error: () => {},
      });
  }

  protected openNew(): void {
    this.editing.set(null);
    const today = new Date().toLocaleDateString('en-CA');
    this.form.reset({
      code: '',
      name: '',
      discountType: 'PERCENT',
      discountValue: 10,
      minimumSubtotal: 0,
      startsOn: today,
      endsOn: today,
      active: true,
    });
    this.dialogVisible.set(true);
  }

  protected openEdit(promotion: Promotion): void {
    this.editing.set(promotion);
    this.form.reset({
      code: promotion.code,
      name: promotion.name,
      discountType: promotion.discountType,
      discountValue: promotion.discountValue,
      minimumSubtotal: promotion.minimumSubtotal,
      startsOn: promotion.startsOn,
      endsOn: promotion.endsOn,
      active: promotion.active,
    });
    this.dialogVisible.set(true);
  }

  protected save(): void {
    const value = this.form.getRawValue();
    if (
      this.form.invalid ||
      value.endsOn < value.startsOn ||
      (value.discountType === 'PERCENT' && value.discountValue > 90) ||
      Math.round(value.discountValue * 100) !== value.discountValue * 100
    ) {
      this.form.markAllAsTouched();
      this.messages.add({
        severity: 'warn',
        summary: 'Revisa el valor y las fechas',
        detail: 'Máximo 90 % y fecha final no anterior a la inicial.',
      });
      return;
    }
    const editing = this.editing();
    const payload = {
      ...value,
      code: value.code.trim().toUpperCase(),
      name: value.name.trim(),
      ...(editing ? { expectedVersion: editing.version } : {}),
    };
    this.saving.set(true);
    const request = editing ? this.api.update(editing.id, payload) : this.api.create(payload);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.dialogVisible.set(false);
        this.load();
        this.messages.add({ severity: 'success', summary: 'Promoción guardada' });
      },
      error: () => this.load(),
    });
  }

  protected validity(p: Promotion): string {
    const today = new Date().toLocaleDateString('en-CA');
    if (!p.active) return 'Pausada';
    if (p.startsOn > today) return 'Programada';
    if (p.endsOn < today) return 'Vencida';
    return 'Vigente';
  }
}
