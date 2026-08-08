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
  protected readonly categories = signal<Category[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly editingCategoryId = signal<number | null>(null);
  protected readonly editingProductId = signal<number | null>(null);
  protected readonly activeCategories = computed(() =>
    this.categories().filter((category) => category.active),
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

  private load(): void {
    this.loading.set(true);
    forkJoin({
      categories: this.catalogApi.findCategories(),
      products: this.catalogApi.findProducts(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ categories, products }) => {
        this.categories.set(categories);
        this.products.set(products);
        if (this.productForm.controls.categoryId.value === null) {
          this.productForm.controls.categoryId.setValue(
            categories.find((category) => category.active)?.id ?? null,
          );
        }
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

  private showSuccess(summary: string): void {
    this.messages.add({
      severity: 'success',
      summary,
      detail: 'Los cambios quedaron guardados.',
    });
  }
}
