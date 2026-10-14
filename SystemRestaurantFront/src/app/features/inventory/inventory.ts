import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { auditTime, finalize, interval, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import {
  Ingredient,
  IngredientPayload,
  IngredientUnit,
  ManualMovementType,
  MovementType,
  StockMovement,
  StockMovementPayload,
} from '../../core/models/inventory.model';
import { InventoryApiService } from '../../core/services/inventory-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';

@Component({
  selector: 'app-inventory',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    ButtonModule,
    InputNumberModule,
    InputTextModule,
    SelectModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './inventory.html',
  styleUrl: './inventory.scss',
})
export class InventoryPage implements OnInit, OnDestroy {
  private readonly api = inject(InventoryApiService);
  private readonly orders = inject(OrderApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly forms = inject(FormBuilder);
  private readonly messages = inject(MessageService);
  private pendingOperationId: string | null = null;
  private realtimeSubscription?: Subscription;
  private branchLookup?: Subscription;
  private fallbackSubscription?: Subscription;
  private refreshGeneration = 0;

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly savingMovement = signal(false);
  protected readonly loadingHistory = signal(false);
  protected readonly ingredients = signal<Ingredient[]>([]);
  protected readonly movements = signal<StockMovement[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly selectedId = signal<number | null>(null);
  protected readonly search = signal('');
  protected readonly lowOnly = signal(false);
  protected readonly selected = computed(
    () => this.ingredients().find((ingredient) => ingredient.id === this.selectedId()) ?? null,
  );
  protected readonly lowCount = computed(
    () => this.ingredients().filter((ingredient) => ingredient.lowStock).length,
  );
  protected readonly visible = computed(() => {
    const search = this.search().trim().toLocaleLowerCase('es');
    return this.ingredients().filter(
      (ingredient) =>
        (!this.lowOnly() || ingredient.lowStock) &&
        (!search || ingredient.name.toLocaleLowerCase('es').includes(search)),
    );
  });

  protected readonly units: { value: IngredientUnit; label: string }[] = [
    { value: 'PIECE', label: 'Piezas' },
    { value: 'KG', label: 'Kilogramos' },
    { value: 'L', label: 'Litros' },
  ];
  protected readonly movementTypes: { value: ManualMovementType; label: string }[] = [
    { value: 'PURCHASE', label: 'Entrada / compra' },
    { value: 'USE', label: 'Salida manual' },
    { value: 'WASTE', label: 'Merma' },
    { value: 'ADJUSTMENT', label: 'Conteo físico' },
  ];

  protected readonly ingredientForm = this.forms.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    unit: this.forms.nonNullable.control<IngredientUnit>('KG', Validators.required),
    minimumQuantity: [0, [Validators.required, Validators.min(0)]],
  });
  protected readonly movementForm = this.forms.nonNullable.group({
    movementType: this.forms.nonNullable.control<ManualMovementType>(
      'PURCHASE',
      Validators.required,
    ),
    quantity: [0, [Validators.required, Validators.min(0)]],
    reason: ['', [Validators.required, Validators.maxLength(250)]],
  });

  constructor() {
    this.movementForm.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.pendingOperationId = null;
    });
  }

  ngOnInit(): void {
    this.refresh();
    this.branchLookup = this.orders.findBranches().subscribe({
      next: (branches) => {
        if (branches.length > 0) {
          this.realtimeSubscription = this.realtime
            .watchBranch(branches[0].id)
            .pipe(auditTime(250))
            .subscribe(() => this.refresh());
        }
      },
      error: () => {},
    });
    this.fallbackSubscription = interval(60_000).subscribe(() => {
      if (!this.saving() && !this.savingMovement()) this.refresh();
    });
  }

  ngOnDestroy(): void {
    this.branchLookup?.unsubscribe();
    this.realtimeSubscription?.unsubscribe();
    this.fallbackSubscription?.unsubscribe();
  }

  protected refresh(): void {
    const generation = ++this.refreshGeneration;
    this.loading.set(true);
    this.api
      .findAll()
      .pipe(
        finalize(() => {
          if (generation === this.refreshGeneration) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (ingredients) => {
          if (generation !== this.refreshGeneration) return;
          this.ingredients.set(ingredients);
          if (this.selectedId() !== null) {
            this.loadHistory(this.selectedId()!);
          }
        },
        error: () => {},
      });
  }

  protected submitIngredient(): void {
    if (this.ingredientForm.invalid) {
      this.ingredientForm.markAllAsTouched();
      this.messages.add({ severity: 'warn', summary: 'Revisa el nombre y el mínimo del insumo.' });
      return;
    }
    const form = this.ingredientForm.getRawValue();
    const current = this.ingredients().find((item) => item.id === this.editingId());
    const payload: IngredientPayload = {
      name: form.name.trim(),
      unit: form.unit,
      minimumQuantity: form.minimumQuantity,
      ...(current ? { version: current.version } : {}),
    };
    const request = current ? this.api.update(current.id, payload) : this.api.create(payload);
    this.saving.set(true);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => {
        this.messages.add({
          severity: 'success',
          summary: current ? 'Insumo actualizado' : 'Insumo creado',
        });
        this.cancelEdit();
        this.selectedId.set(saved.id);
        this.refresh();
      },
      error: () => {},
    });
  }

  protected edit(ingredient: Ingredient): void {
    this.editingId.set(ingredient.id);
    this.ingredientForm.setValue({
      name: ingredient.name,
      unit: ingredient.unit,
      minimumQuantity: ingredient.minimumQuantity,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.ingredientForm.reset({ name: '', unit: 'KG', minimumQuantity: 0 });
  }

  protected select(ingredient: Ingredient): void {
    this.pendingOperationId = null;
    this.selectedId.set(ingredient.id);
    this.movements.set([]);
    this.loadHistory(ingredient.id);
  }

  protected changeActive(ingredient: Ingredient): void {
    this.api.changeActive(ingredient.id, !ingredient.active).subscribe({
      next: () => {
        this.messages.add({ severity: 'success', summary: 'Estado del insumo actualizado' });
        this.refresh();
      },
      error: () => {},
    });
  }

  protected submitMovement(): void {
    if (this.savingMovement()) return;
    const ingredient = this.selected();
    if (!ingredient || this.movementForm.invalid) {
      this.movementForm.markAllAsTouched();
      this.messages.add({ severity: 'warn', summary: 'Indica cantidad y motivo del movimiento.' });
      return;
    }
    const form = this.movementForm.getRawValue();
    if (form.movementType !== 'ADJUSTMENT' && form.quantity <= 0) {
      this.messages.add({ severity: 'warn', summary: 'La cantidad debe ser mayor a cero.' });
      return;
    }
    this.pendingOperationId ??= crypto.randomUUID();
    const payload: StockMovementPayload = {
      operationId: this.pendingOperationId,
      movementType: form.movementType,
      quantity: form.quantity,
      reason: form.reason.trim(),
      expectedVersion: ingredient.version,
    };
    this.savingMovement.set(true);
    this.api
      .recordMovement(ingredient.id, payload)
      .pipe(finalize(() => this.savingMovement.set(false)))
      .subscribe({
        next: () => {
          this.pendingOperationId = null;
          this.movementForm.reset({ movementType: 'PURCHASE', quantity: 0, reason: '' });
          this.messages.add({ severity: 'success', summary: 'Movimiento registrado' });
          this.refresh();
        },
        error: () => {},
      });
  }

  protected unitLabel(unit: IngredientUnit): string {
    return this.units.find((option) => option.value === unit)?.label ?? unit;
  }

  protected movementLabel(type: MovementType): string {
    if (type === 'ORDER_CONSUMPTION') return 'Consumo por comanda';
    if (type === 'ORDER_REVERSAL') return 'Devolución por cancelación';
    return this.movementTypes.find((option) => option.value === type)?.label ?? type;
  }

  private loadHistory(id: number): void {
    this.loadingHistory.set(true);
    this.api
      .findMovements(id)
      .pipe(finalize(() => this.loadingHistory.set(false)))
      .subscribe({
        next: (items) => {
          if (this.selectedId() === id) {
            this.movements.set(items);
          }
        },
        error: () => {},
      });
  }
}
