import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import {
  Category,
  CategoryPayload,
  ModifierGroup,
  ModifierGroupPayload,
  ModifierOption,
  ModifierOptionPayload,
  Product,
  ProductDestination,
  ProductPayload,
} from '../../core/models/catalog.model';
import { CatalogApiService } from '../../core/services/catalog-api.service';

interface DestinationOption {
  label: string;
  value: ProductDestination;
}

@Component({
  selector: 'app-catalog',
  imports: [
    ButtonModule,
    CardModule,
    CurrencyPipe,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    SelectModule,
    TableModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly catalogApi = inject(CatalogApiService);
  private readonly messages = inject(MessageService);

  protected readonly loading = signal(true);
  protected readonly savingCategory = signal(false);
  protected readonly savingProduct = signal(false);
  protected readonly savingModifierGroup = signal(false);
  protected readonly savingModifierOption = signal(false);
  protected readonly categories = signal<Category[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly modifierGroups = signal<ModifierGroup[]>([]);
  protected readonly editingCategoryId = signal<number | null>(null);
  protected readonly editingProductId = signal<number | null>(null);
  protected readonly editingModifierGroupId = signal<number | null>(null);
  protected readonly editingModifierOptionId = signal<number | null>(null);
  protected readonly activeCategories = computed(() =>
    this.categories().filter((category) => category.active),
  );
  protected readonly activeProducts = computed(() => {
    const activeCategoryIds = new Set(this.activeCategories().map((category) => category.id));
    return this.products().filter(
      (product) => product.active && activeCategoryIds.has(product.categoryId),
    );
  });
  protected readonly activeModifierGroups = computed(() =>
    this.modifierGroups().filter((group) => group.active),
  );

  protected readonly destinations: DestinationOption[] = [
    { label: 'Producción o preparación', value: 'PRODUCTION' },
    { label: 'Servicio o entrega', value: 'SERVICE' },
    { label: 'Sin ruta operativa', value: 'NONE' },
  ];

  protected readonly categoryForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    description: ['', Validators.maxLength(250)],
    sortOrder: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly productForm = this.formBuilder.group({
    categoryId: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
    ]),
    sku: this.formBuilder.nonNullable.control('', Validators.maxLength(40)),
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(120),
    ]),
    description: this.formBuilder.nonNullable.control('', Validators.maxLength(500)),
    price: this.formBuilder.control<number | null>(null, [Validators.required, Validators.min(0)]),
    destination: this.formBuilder.nonNullable.control<ProductDestination>('PRODUCTION', [
      Validators.required,
    ]),
  });

  protected readonly modifierGroupForm = this.formBuilder.group({
    productId: this.formBuilder.control<number | null>(null, Validators.required),
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(100),
    ]),
    minSelections: this.formBuilder.nonNullable.control(0, [
      Validators.required,
      Validators.min(0),
      Validators.max(20),
    ]),
    maxSelections: this.formBuilder.nonNullable.control(1, [
      Validators.required,
      Validators.min(1),
      Validators.max(20),
    ]),
    sortOrder: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(0)]),
  });

  protected readonly modifierOptionForm = this.formBuilder.group({
    groupId: this.formBuilder.control<number | null>(null, Validators.required),
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(100),
    ]),
    priceDelta: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(0)]),
    sortOrder: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(0)]),
  });

  ngOnInit(): void {
    this.load();
  }

  protected submitCategory(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    this.savingCategory.set(true);
    const payload: CategoryPayload = this.categoryForm.getRawValue();
    const editingId = this.editingCategoryId();
    const request = editingId
      ? this.catalogApi.updateCategory(editingId, payload)
      : this.catalogApi.createCategory(payload);

    request.pipe(finalize(() => this.savingCategory.set(false))).subscribe((category) => {
      this.categories.update((items) => this.upsertCategory(items, category));
      if (this.productForm.controls.categoryId.value === null && category.active) {
        this.productForm.controls.categoryId.setValue(category.id);
      }
      this.cancelCategoryEdit();
      this.showSuccess(editingId ? 'Categoría actualizada' : 'Categoría creada');
    });
  }

  protected editCategory(category: Category): void {
    this.editingCategoryId.set(category.id);
    this.categoryForm.setValue({
      name: category.name,
      description: category.description ?? '',
      sortOrder: category.sortOrder,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelCategoryEdit(): void {
    this.editingCategoryId.set(null);
    this.categoryForm.reset({
      name: '',
      description: '',
      sortOrder: 0,
    });
  }

  protected toggleCategory(category: Category): void {
    this.catalogApi
      .changeCategoryActive(category.id, !category.active)
      .subscribe((updated) =>
        this.categories.update((items) => this.upsertCategory(items, updated)),
      );
  }

  protected submitProduct(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    const value = this.productForm.getRawValue();
    const payload: ProductPayload = {
      categoryId: value.categoryId as number,
      sku: value.sku,
      name: value.name,
      description: value.description,
      price: value.price as number,
      destination: value.destination,
    };

    this.savingProduct.set(true);
    const editingId = this.editingProductId();
    const request = editingId
      ? this.catalogApi.updateProduct(editingId, payload)
      : this.catalogApi.createProduct(payload);

    request.pipe(finalize(() => this.savingProduct.set(false))).subscribe((product) => {
      this.products.update((items) => this.upsertProduct(items, product));
      this.cancelProductEdit();
      this.showSuccess(editingId ? 'Producto actualizado' : 'Producto creado');
    });
  }

  protected editProduct(product: Product): void {
    this.editingProductId.set(product.id);
    this.productForm.setValue({
      categoryId: product.categoryId,
      sku: product.sku ?? '',
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      destination: product.destination,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelProductEdit(): void {
    this.editingProductId.set(null);
    this.productForm.reset({
      categoryId: this.activeCategories()[0]?.id ?? null,
      sku: '',
      name: '',
      description: '',
      price: null,
      destination: 'PRODUCTION',
    });
  }

  protected toggleProductActive(product: Product): void {
    this.catalogApi.changeProductActive(product.id, !product.active).subscribe((updated) => {
      this.products.update((items) => this.upsertProduct(items, updated));
    });
  }

  protected toggleAvailability(product: Product): void {
    this.catalogApi
      .changeProductAvailability(product.id, !product.available)
      .subscribe((updated) => {
        this.products.update((items) => this.upsertProduct(items, updated));
      });
  }

  protected destinationLabel(destination: ProductDestination): string {
    return this.destinations.find((option) => option.value === destination)?.label ?? destination;
  }

  protected submitModifierGroup(): void {
    if (this.modifierGroupForm.invalid) {
      this.modifierGroupForm.markAllAsTouched();
      return;
    }
    const value = this.modifierGroupForm.getRawValue();
    if (value.minSelections > value.maxSelections) {
      this.messages.add({
        severity: 'warn',
        summary: 'Regla inválida',
        detail: 'La selección mínima no puede ser mayor que la máxima.',
      });
      return;
    }
    const payload: ModifierGroupPayload = {
      productId: value.productId as number,
      name: value.name,
      minSelections: value.minSelections,
      maxSelections: value.maxSelections,
      sortOrder: value.sortOrder,
    };
    const editingId = this.editingModifierGroupId();
    const request = editingId
      ? this.catalogApi.updateModifierGroup(editingId, payload)
      : this.catalogApi.createModifierGroup(payload);
    this.savingModifierGroup.set(true);
    request.pipe(finalize(() => this.savingModifierGroup.set(false))).subscribe((group) => {
      this.modifierGroups.update((items) => this.upsertModifierGroup(items, group));
      if (this.modifierOptionForm.controls.groupId.value === null && group.active) {
        this.modifierOptionForm.controls.groupId.setValue(group.id);
      }
      this.cancelModifierGroupEdit();
      this.showSuccess(editingId ? 'Grupo actualizado' : 'Grupo creado');
    });
  }

  protected editModifierGroup(group: ModifierGroup): void {
    this.editingModifierGroupId.set(group.id);
    this.modifierGroupForm.setValue({
      productId: group.productId,
      name: group.name,
      minSelections: group.minSelections,
      maxSelections: group.maxSelections,
      sortOrder: group.sortOrder,
    });
    document.getElementById('modifier-editor')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected cancelModifierGroupEdit(): void {
    this.editingModifierGroupId.set(null);
    this.modifierGroupForm.reset({
      productId: this.activeProducts()[0]?.id ?? null,
      name: '',
      minSelections: 0,
      maxSelections: 1,
      sortOrder: 0,
    });
  }

  protected toggleModifierGroup(group: ModifierGroup): void {
    this.catalogApi.changeModifierGroupActive(group.id, !group.active).subscribe((updated) => {
      this.modifierGroups.update((items) => this.upsertModifierGroup(items, updated));
    });
  }

  protected submitModifierOption(): void {
    if (this.modifierOptionForm.invalid) {
      this.modifierOptionForm.markAllAsTouched();
      return;
    }
    const value = this.modifierOptionForm.getRawValue();
    const payload: ModifierOptionPayload = {
      groupId: value.groupId as number,
      name: value.name,
      priceDelta: value.priceDelta,
      sortOrder: value.sortOrder,
    };
    const editingId = this.editingModifierOptionId();
    const request = editingId
      ? this.catalogApi.updateModifierOption(editingId, payload)
      : this.catalogApi.createModifierOption(payload);
    this.savingModifierOption.set(true);
    request.pipe(finalize(() => this.savingModifierOption.set(false))).subscribe((option) => {
      this.modifierGroups.update((groups) => this.upsertModifierOption(groups, option));
      this.cancelModifierOptionEdit();
      this.showSuccess(editingId ? 'Opción actualizada' : 'Opción creada');
    });
  }

  protected editModifierOption(option: ModifierOption): void {
    this.editingModifierOptionId.set(option.id);
    this.modifierOptionForm.setValue({
      groupId: option.groupId,
      name: option.name,
      priceDelta: option.priceDelta,
      sortOrder: option.sortOrder,
    });
    this.modifierOptionForm.controls.groupId.disable();
    document.getElementById('modifier-editor')?.scrollIntoView({ behavior: 'smooth' });
  }

  protected cancelModifierOptionEdit(): void {
    this.editingModifierOptionId.set(null);
    this.modifierOptionForm.controls.groupId.enable();
    this.modifierOptionForm.reset({
      groupId: this.activeModifierGroups()[0]?.id ?? null,
      name: '',
      priceDelta: 0,
      sortOrder: 0,
    });
  }

  protected toggleModifierOption(option: ModifierOption): void {
    this.catalogApi.changeModifierOptionActive(option.id, !option.active).subscribe((updated) => {
      this.modifierGroups.update((groups) => this.upsertModifierOption(groups, updated));
    });
  }

  protected selectionRule(group: ModifierGroup): string {
    if (group.minSelections === group.maxSelections) {
      return `Exactamente ${group.minSelections}`;
    }
    return `${group.minSelections} a ${group.maxSelections}`;
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      categories: this.catalogApi.findCategories(),
      products: this.catalogApi.findProducts(),
      modifierGroups: this.catalogApi.findModifierGroups(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ categories, products, modifierGroups }) => {
        this.categories.set(categories);
        this.products.set(products);
        this.modifierGroups.set(modifierGroups);
        if (this.productForm.controls.categoryId.value === null) {
          this.productForm.controls.categoryId.setValue(
            categories.find((category) => category.active)?.id ?? null,
          );
        }
        this.modifierGroupForm.controls.productId.setValue(
          products.find((product) => product.active)?.id ?? null,
        );
        this.modifierOptionForm.controls.groupId.setValue(
          modifierGroups.find((group) => group.active)?.id ?? null,
        );
      });
  }

  private upsertCategory(items: Category[], updated: Category): Category[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort(
      (left, right) => left.sortOrder - right.sortOrder || left.name.localeCompare(right.name),
    );
  }

  private upsertProduct(items: Product[], updated: Product): Product[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort((left, right) => left.name.localeCompare(right.name));
  }

  private upsertModifierGroup(items: ModifierGroup[], updated: ModifierGroup): ModifierGroup[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort(
      (left, right) =>
        left.productName.localeCompare(right.productName) ||
        left.sortOrder - right.sortOrder ||
        left.name.localeCompare(right.name),
    );
  }

  private upsertModifierOption(groups: ModifierGroup[], updated: ModifierOption): ModifierGroup[] {
    return groups.map((group) => {
      if (group.id !== updated.groupId) {
        return group;
      }
      const options = group.options.some((option) => option.id === updated.id)
        ? group.options.map((option) => (option.id === updated.id ? updated : option))
        : [...group.options, updated];
      return {
        ...group,
        options: options.sort(
          (left, right) => left.sortOrder - right.sortOrder || left.name.localeCompare(right.name),
        ),
      };
    });
  }

  private showSuccess(summary: string): void {
    this.messages.add({
      severity: 'success',
      summary,
      detail: 'Los cambios quedaron guardados.',
    });
  }
}
