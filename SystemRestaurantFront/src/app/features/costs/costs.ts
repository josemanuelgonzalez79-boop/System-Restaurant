import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { auditTime, finalize, forkJoin, interval, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import { CostChange, ProductCost, RecipeCost } from '../../core/models/cost.model';
import { Ingredient, IngredientUnit } from '../../core/models/inventory.model';
import { CostApiService } from '../../core/services/cost-api.service';
import { InventoryApiService } from '../../core/services/inventory-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';

@Component({
  selector: 'app-costs',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    ButtonModule,
    InputNumberModule,
    InputTextModule,
    SelectModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './costs.html',
  styleUrl: './costs.scss',
})
export class CostsPage implements OnInit, OnDestroy {
  private readonly api = inject(CostApiService);
  private readonly inventory = inject(InventoryApiService);
  private readonly orders = inject(OrderApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly messages = inject(MessageService);
  private branchLookup?: Subscription;
  private realtimeSubscription?: Subscription;
  private fallbackSubscription?: Subscription;
  private generation = 0;

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly catalog = signal<ProductCost[]>([]);
  protected readonly ingredients = signal<Ingredient[]>([]);
  protected readonly selectedProductId = signal<number | null>(null);
  protected readonly selectedIngredientId = signal<number | null>(null);
  protected readonly history = signal<CostChange[]>([]);
  protected readonly loadingHistory = signal(false);
  protected readonly search = signal('');
  protected readonly selectedProduct = computed(
    () => this.catalog().find((product) => product.id === this.selectedProductId()) ?? null,
  );
  protected readonly selectedIngredient = computed(
    () => this.ingredients().find((item) => item.id === this.selectedIngredientId()) ?? null,
  );
  protected readonly visibleProducts = computed(() => {
    const term = this.search().trim().toLocaleLowerCase('es');
    return this.catalog().filter(
      (product) =>
        !term || `${product.name} ${product.categoryName}`.toLocaleLowerCase('es').includes(term),
    );
  });
  protected readonly pricedIngredients = computed(
    () => this.ingredients().filter((ingredient) => ingredient.unitCost !== null).length,
  );
  protected readonly completeProducts = computed(
    () => this.catalog().filter((product) => product.recipe.cost !== null).length,
  );
  protected draftCost: number | null = null;
  protected draftReason = '';
  protected editingVersion: number | null = null;

  ngOnInit(): void {
    this.refresh();
    this.branchLookup = this.orders.findBranches().subscribe({
      next: (branches) => {
        if (branches.length > 0) {
          this.realtimeSubscription = this.realtime
            .watchBranch(branches[0].id)
            .pipe(auditTime(300))
            .subscribe(() => this.refresh());
        }
      },
      error: () => {},
    });
    this.fallbackSubscription = interval(60_000).subscribe(() => {
      if (!this.saving()) this.refresh();
    });
  }

  ngOnDestroy(): void {
    this.branchLookup?.unsubscribe();
    this.realtimeSubscription?.unsubscribe();
    this.fallbackSubscription?.unsubscribe();
  }

  protected refresh(): void {
    const generation = ++this.generation;
    this.loading.set(true);
    forkJoin({ products: this.api.catalog(), ingredients: this.inventory.findAll() })
      .pipe(
        finalize(() => {
          if (generation === this.generation) this.loading.set(false);
        }),
      )
      .subscribe({
        next: ({ products, ingredients }) => {
          if (generation !== this.generation) return;
          this.catalog.set(products);
          this.ingredients.set(ingredients);
          if (!products.some((product) => product.id === this.selectedProductId())) {
            this.selectedProductId.set(products[0]?.id ?? null);
          }
          if (!ingredients.some((item) => item.id === this.selectedIngredientId())) {
            this.selectIngredient(ingredients[0]?.id ?? null);
          }
        },
        error: () => {},
      });
  }

  protected selectIngredient(id: number | null): void {
    this.selectedIngredientId.set(id);
    const selected = this.selectedIngredient();
    this.draftCost = selected?.unitCost ?? null;
    this.editingVersion = selected?.version ?? null;
    this.draftReason = '';
    this.loadHistory(id);
  }

  protected selectProduct(id: number): void {
    this.selectedProductId.set(id);
  }

  protected saveCost(): void {
    const ingredient = this.selectedIngredient();
    const cost = this.draftCost;
    const reason = this.draftReason.trim();
    if (!ingredient || this.editingVersion === null || this.saving()) return;
    if (
      cost === null ||
      !Number.isFinite(cost) ||
      cost < 0 ||
      cost > 9999999999.9999 ||
      Math.abs(Math.round(cost * 10000) - cost * 10000) > 0.0001 ||
      reason.length < 5 ||
      reason.length > 250
    ) {
      this.messages.add({
        severity: 'warn',
        summary: 'Revisa el costo y el motivo (5 a 250 caracteres).',
      });
      return;
    }
    this.saving.set(true);
    this.api
      .update(ingredient.id, { unitCost: cost, expectedVersion: this.editingVersion, reason })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => {
          if (this.selectedIngredientId() === saved.id) {
            this.editingVersion = saved.version;
            this.draftCost = saved.unitCost;
            this.draftReason = '';
            this.loadHistory(saved.id);
          }
          this.messages.add({ severity: 'success', summary: 'Costo actualizado y registrado' });
          this.refresh();
        },
        error: () => {},
      });
  }

  protected costStatus(recipe: RecipeCost): string {
    if (!recipe.configured) return 'Sin receta';
    if (recipe.cost === null) return 'Faltan costos';
    return 'Calculado';
  }

  protected unitLabel(unit: IngredientUnit): string {
    return unit === 'PIECE' ? 'pieza' : unit === 'KG' ? 'kg' : 'L';
  }

  private loadHistory(id: number | null): void {
    this.history.set([]);
    if (id === null) return;
    this.loadingHistory.set(true);
    this.api
      .history(id)
      .pipe(
        finalize(() => {
          if (this.selectedIngredientId() === id) this.loadingHistory.set(false);
        }),
      )
      .subscribe({
        next: (history) => {
          if (this.selectedIngredientId() === id) this.history.set(history);
        },
        error: () => {},
      });
  }
}
