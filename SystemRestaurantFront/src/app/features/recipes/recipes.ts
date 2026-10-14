import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';

import { ModifierGroup, Product } from '../../core/models/catalog.model';
import { Ingredient, IngredientUnit } from '../../core/models/inventory.model';
import { Recipe, RecipeOwnerType, RecipeSavePayload } from '../../core/models/recipe.model';
import { CatalogApiService } from '../../core/services/catalog-api.service';
import { InventoryApiService } from '../../core/services/inventory-api.service';
import { RecipeApiService } from '../../core/services/recipe-api.service';

interface RecipeTarget {
  key: string;
  id: number;
  type: RecipeOwnerType;
  kind: string;
  label: string;
  active: boolean;
}

interface DraftLine {
  ingredientId: number;
  quantity: number | null;
}

@Component({
  selector: 'app-recipes',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    ButtonModule,
    InputNumberModule,
    SelectModule,
    TagModule,
  ],
  templateUrl: './recipes.html',
  styleUrl: './recipes.scss',
})
export class RecipesPage implements OnInit {
  private readonly catalog = inject(CatalogApiService);
  private readonly inventory = inject(InventoryApiService);
  private readonly recipes = inject(RecipeApiService);
  private readonly messages = inject(MessageService);

  protected readonly loading = signal(true);
  protected readonly loadingRecipe = signal(false);
  protected readonly saving = signal(false);
  protected readonly products = signal<Product[]>([]);
  protected readonly groups = signal<ModifierGroup[]>([]);
  protected readonly ingredients = signal<Ingredient[]>([]);
  protected readonly selectedKey = signal<string | null>(null);
  protected readonly recipe = signal<Recipe | null>(null);
  protected readonly draft = signal<DraftLine[]>([]);
  protected addIngredientId: number | null = null;
  protected addQuantity: number | null = 1;

  protected readonly targets = computed<RecipeTarget[]>(() => [
    ...this.products().map((product) => ({
      key: `P:${product.id}`,
      id: product.id,
      type: 'PRODUCT' as const,
      kind: 'Producto',
      label: product.name,
      active: product.active,
    })),
    ...this.groups().flatMap((group) =>
      group.options.map((option) => ({
        key: `O:${option.id}`,
        id: option.id,
        type: 'MODIFIER_OPTION' as const,
        kind: 'Opción o extra',
        label: `${group.productName} · ${group.name} · ${option.name}`,
        active: group.active && option.active,
      })),
    ),
  ]);
  protected readonly selected = computed(
    () => this.targets().find((target) => target.key === this.selectedKey()) ?? null,
  );
  protected readonly availableIngredients = computed(() => {
    const selectedIds = new Set(this.draft().map((line) => line.ingredientId));
    return this.ingredients().filter(
      (ingredient) => ingredient.active && !selectedIds.has(ingredient.id),
    );
  });
  protected readonly hasChanges = computed(() => {
    const current = this.recipe();
    const lines = this.draft();
    return (
      !!current &&
      (current.lines.length !== lines.length ||
        lines.some(
          (line, index) =>
            line.ingredientId !== current.lines[index]?.ingredientId ||
            line.quantity !== current.lines[index]?.quantity,
        ))
    );
  });

  ngOnInit(): void {
    this.refresh();
  }

  protected refresh(): void {
    this.loading.set(true);
    forkJoin({
      products: this.catalog.findProducts(),
      groups: this.catalog.findModifierGroups(),
      ingredients: this.inventory.findAll(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ products, groups, ingredients }) => {
          this.products.set(products);
          this.groups.set(groups);
          this.ingredients.set(ingredients);
          const key =
            this.selectedKey() && this.targets().some((t) => t.key === this.selectedKey())
              ? this.selectedKey()
              : (this.targets()[0]?.key ?? null);
          this.chooseTarget(key);
        },
        error: () => {},
      });
  }

  protected chooseTarget(key: string | null): void {
    this.selectedKey.set(key);
    this.recipe.set(null);
    this.draft.set([]);
    this.addIngredientId = null;
    this.addQuantity = 1;
    const target = this.targets().find((option) => option.key === key);
    if (!target) return;
    this.loadingRecipe.set(true);
    this.recipes
      .find(target.type, target.id)
      .pipe(
        finalize(() => {
          if (this.selectedKey() === target.key) this.loadingRecipe.set(false);
        }),
      )
      .subscribe({
        next: (recipe) => {
          if (this.selectedKey() !== target.key) return;
          this.recipe.set(recipe);
          this.draft.set(
            recipe.lines.map((line) => ({
              ingredientId: line.ingredientId,
              quantity: line.quantity,
            })),
          );
        },
        error: () => {},
      });
  }

  protected addLine(): void {
    const ingredient = this.ingredientFor(this.addIngredientId);
    const quantity = this.addQuantity;
    if (!ingredient || !ingredient.active || !this.validQuantity(quantity, ingredient.unit)) {
      this.messages.add({
        severity: 'warn',
        summary: 'Selecciona un insumo y una cantidad válida.',
      });
      return;
    }
    if (this.draft().some((line) => line.ingredientId === ingredient.id)) {
      this.messages.add({ severity: 'warn', summary: 'Ese insumo ya está en la receta.' });
      return;
    }
    if (this.draft().length >= 50) {
      this.messages.add({ severity: 'warn', summary: 'Máximo 50 insumos por receta.' });
      return;
    }
    this.draft.update((lines) => [...lines, { ingredientId: ingredient.id, quantity }]);
    this.addIngredientId = null;
    this.addQuantity = 1;
  }

  protected changeQuantity(ingredientId: number, quantity: number | null): void {
    this.draft.update((lines) =>
      lines.map((line) => (line.ingredientId === ingredientId ? { ...line, quantity } : line)),
    );
  }

  protected removeLine(ingredientId: number): void {
    this.draft.update((lines) => lines.filter((line) => line.ingredientId !== ingredientId));
  }

  protected discard(): void {
    this.draft.set(
      (this.recipe()?.lines ?? []).map((line) => ({
        ingredientId: line.ingredientId,
        quantity: line.quantity,
      })),
    );
  }

  protected save(): void {
    const target = this.selected();
    const current = this.recipe();
    if (!target || !current || this.loadingRecipe() || this.saving()) return;
    if (this.draft().length === 0 && current.id === null) {
      this.messages.add({
        severity: 'warn',
        summary: 'Agrega un insumo antes de crear la receta.',
      });
      return;
    }
    for (const line of this.draft()) {
      const ingredient = this.ingredientFor(line.ingredientId);
      if (
        !ingredient ||
        !ingredient.active ||
        !this.validQuantity(line.quantity, ingredient.unit)
      ) {
        this.messages.add({
          severity: 'warn',
          summary: 'Revisa las cantidades y el estado de los insumos.',
        });
        return;
      }
    }
    const payload: RecipeSavePayload = {
      expectedVersion: current.version,
      lines: this.draft().map((line) => ({
        ingredientId: line.ingredientId,
        quantity: line.quantity!,
      })),
    };
    this.saving.set(true);
    this.recipes
      .save(target.type, target.id, payload)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => {
          if (this.selectedKey() !== target.key) return;
          this.recipe.set(saved);
          this.draft.set(
            saved.lines.map((line) => ({
              ingredientId: line.ingredientId,
              quantity: line.quantity,
            })),
          );
          this.messages.add({ severity: 'success', summary: 'Receta guardada' });
        },
        error: () => {},
      });
  }

  protected ingredientFor(id: number | null): Ingredient | undefined {
    return this.ingredients().find((ingredient) => ingredient.id === id);
  }

  protected unitLabel(unit: IngredientUnit): string {
    return unit === 'PIECE' ? 'pzas' : unit === 'KG' ? 'kg' : 'L';
  }

  protected validQuantity(quantity: number | null, unit: IngredientUnit): boolean {
    return (
      quantity !== null &&
      Number.isFinite(quantity) &&
      quantity > 0 &&
      quantity <= 99999999999.999 &&
      Math.abs(Math.round(quantity * 1000) - quantity * 1000) < 1e-7 &&
      (unit !== 'PIECE' || Number.isInteger(quantity))
    );
  }
}
