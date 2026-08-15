import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import { Category, ModifierGroup, ModifierOption, Product } from '../../core/models/catalog.model';
import { OrderDetail, OrderItem } from '../../core/models/order.model';
import { CatalogApiService } from '../../core/services/catalog-api.service';
import { OrderApiService } from '../../core/services/order-api.service';

@Component({
  selector: 'app-order-detail',
  imports: [
    ButtonModule,
    ConfirmDialogModule,
    CurrencyPipe,
    DialogModule,
    FormsModule,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    TagModule,
    TextareaModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly orderApi = inject(OrderApiService);
  private readonly catalogApi = inject(CatalogApiService);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);

  private readonly orderId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly saving = signal(false);
  protected readonly dispatching = signal(false);
  protected readonly deletingItemId = signal<number | null>(null);
  protected readonly detail = signal<OrderDetail | null>(null);
  protected readonly categories = signal<Category[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly modifierGroups = signal<ModifierGroup[]>([]);
  protected readonly selectedCategoryId = signal<number | null>(null);
  protected readonly search = signal('');
  protected readonly editorVisible = signal(false);
  protected readonly selectedProduct = signal<Product | null>(null);
  protected readonly editingItem = signal<OrderItem | null>(null);
  protected readonly selectedOptionIds = signal<Set<number>>(new Set());

  protected readonly activeCategories = computed(() =>
    this.categories().filter((category) => category.active),
  );
  protected readonly visibleProducts = computed(() => {
    const categoryId = this.selectedCategoryId();
    const term = this.search().trim().toLocaleLowerCase('es-MX');
    const activeCategoryIds = new Set(this.activeCategories().map((category) => category.id));
    return this.products().filter(
      (product) =>
        product.active &&
        product.available &&
        activeCategoryIds.has(product.categoryId) &&
        (categoryId === null || product.categoryId === categoryId) &&
        (!term ||
          product.name.toLocaleLowerCase('es-MX').includes(term) ||
          product.sku?.toLocaleLowerCase('es-MX').includes(term)),
    );
  });
  protected readonly editorGroups = computed(() => {
    const productId = this.selectedProduct()?.id;
    return productId === undefined
      ? []
      : this.modifierGroups().filter((group) => group.productId === productId && group.active);
  });
  protected readonly canEdit = computed(() => {
    const status = this.detail()?.order.status;
    return status === 'OPEN' || status === 'IN_PROGRESS';
  });
  protected readonly pendingDispatchCount = computed(
    () =>
      this.detail()?.items.filter(
        (item) => !item.sentAt && (item.destination === 'PRODUCTION' || item.destination === 'SERVICE'),
      ).length ?? 0,
  );

  protected readonly itemForm = this.formBuilder.nonNullable.group({
    quantity: [1, [Validators.required, Validators.min(1), Validators.max(99)]],
    notes: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    if (!Number.isInteger(this.orderId) || this.orderId <= 0) {
      void this.router.navigate(['/pedidos']);
      return;
    }
    this.loadAll();
  }

  protected chooseCategory(categoryId: number | null): void {
    this.selectedCategoryId.set(categoryId);
  }

  protected openProduct(product: Product): void {
    if (!this.canEdit()) {
      return;
    }
    this.editingItem.set(null);
    this.selectedProduct.set(product);
    this.selectedOptionIds.set(new Set());
    this.itemForm.reset({ quantity: 1, notes: '' });
    this.editorVisible.set(true);
  }

  protected editItem(item: OrderItem): void {
    if (item.sentAt) {
      this.messages.add({
        severity: 'warn',
        summary: 'Partida enviada',
        detail: 'Ya está en preparación. Captura una partida adicional si el cliente pide algo más.',
      });
      return;
    }
    const product = this.products().find((candidate) => candidate.id === item.productId);
    if (!product) {
      this.messages.add({
        severity: 'warn',
        summary: 'Producto histórico',
        detail: 'El producto ya no existe en el catálogo y no puede editarse; sí puedes retirarlo.',
      });
      return;
    }
    this.editingItem.set(item);
    this.selectedProduct.set(product);
    this.selectedOptionIds.set(
      new Set(item.modifiers.map((modifier) => modifier.modifierOptionId)),
    );
    this.itemForm.reset({ quantity: item.quantity, notes: item.notes ?? '' });
    this.editorVisible.set(true);
  }

  protected closeEditor(): void {
    this.editorVisible.set(false);
    this.selectedProduct.set(null);
    this.editingItem.set(null);
    this.selectedOptionIds.set(new Set());
  }

  protected toggleOption(group: ModifierGroup, option: ModifierOption): void {
    const current = new Set(this.selectedOptionIds());
    if (current.has(option.id)) {
      current.delete(option.id);
      this.selectedOptionIds.set(current);
      return;
    }

    const groupOptionIds = new Set(group.options.map((item) => item.id));
    const selectedInGroup = [...current].filter((id) => groupOptionIds.has(id));
    if (group.maxSelections === 1) {
      selectedInGroup.forEach((id) => current.delete(id));
    } else if (selectedInGroup.length >= group.maxSelections) {
      this.messages.add({
        severity: 'warn',
        summary: group.name,
        detail: `Puedes elegir como máximo ${group.maxSelections} opción(es).`,
      });
      return;
    }
    current.add(option.id);
    this.selectedOptionIds.set(current);
  }

  protected isOptionSelected(optionId: number): boolean {
    return this.selectedOptionIds().has(optionId);
  }

  protected selectedCount(group: ModifierGroup): number {
    const optionIds = new Set(group.options.map((option) => option.id));
    return [...this.selectedOptionIds()].filter((id) => optionIds.has(id)).length;
  }

  protected modifierRule(group: ModifierGroup): string {
    if (group.minSelections === 0 && group.maxSelections === 1) {
      return 'Opcional · elige hasta 1';
    }
    if (group.minSelections === group.maxSelections) {
      return `Elige ${group.minSelections}`;
    }
    return `Elige de ${group.minSelections} a ${group.maxSelections}`;
  }

  protected submitItem(): void {
    const detail = this.detail();
    const product = this.selectedProduct();
    if (!detail || !product || this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }
    const modifierError = this.modifierError();
    if (modifierError) {
      this.messages.add({
        severity: 'warn',
        summary: 'Falta una selección',
        detail: modifierError,
      });
      return;
    }

    const value = this.itemForm.getRawValue();
    const payload = {
      quantity: value.quantity,
      notes: value.notes,
      modifierOptionIds: [...this.selectedOptionIds()],
      orderVersion: detail.order.version,
    };
    const editing = this.editingItem();
    const request = editing
      ? this.orderApi.updateItem(this.orderId, editing.id, payload)
      : this.orderApi.addItem(this.orderId, { productId: product.id, ...payload });

    this.saving.set(true);
    request.pipe(finalize(() => this.saving.set(false))).subscribe((updated) => {
      this.detail.set(updated);
      this.closeEditor();
      this.messages.add({
        severity: 'success',
        summary: editing ? 'Producto actualizado' : 'Producto agregado',
        detail: `${product.name} quedó guardado en ${updated.order.folio}.`,
      });
    });
  }

  protected removeItem(item: OrderItem): void {
    const detail = this.detail();
    if (!detail || item.sentAt) {
      return;
    }
    this.confirmations.confirm({
      header: 'Retirar producto',
      message: `¿Deseas retirar ${item.quantity} × ${item.productName} del pedido?`,
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: 'Conservar',
      acceptLabel: 'Retirar',
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.deletingItemId.set(item.id);
        this.orderApi
          .removeItem(this.orderId, item.id, detail.order.version)
          .pipe(finalize(() => this.deletingItemId.set(null)))
          .subscribe((updated) => {
            this.detail.set(updated);
            this.messages.add({
              severity: 'success',
              summary: 'Producto retirado',
              detail: 'La cuenta fue recalculada.',
            });
          });
      },
    });
  }

  protected refresh(): void {
    this.refreshing.set(true);
    this.orderApi
      .findDetail(this.orderId)
      .pipe(finalize(() => this.refreshing.set(false)))
      .subscribe((detail) => this.detail.set(detail));
  }

  protected dispatch(): void {
    const current = this.detail();
    const count = this.pendingDispatchCount();
    if (!current || count === 0) {
      return;
    }
    this.confirmations.confirm({
      header: 'Enviar comanda',
      message:
        `Se enviarán ${count} partida(s) nueva(s) a cocina o servicio. ` +
        'Después ya no podrán editarse ni retirarse desde la cuenta.',
      icon: 'pi pi-send',
      rejectLabel: 'Revisar pedido',
      acceptLabel: 'Enviar comanda',
      accept: () => {
        this.dispatching.set(true);
        this.orderApi
          .dispatch(this.orderId, current.order.version)
          .pipe(finalize(() => this.dispatching.set(false)))
          .subscribe((response) => {
            this.detail.set(response.orderDetail);
            const routes = response.tickets
              .map((ticket) => (ticket.destination === 'PRODUCTION' ? 'cocina' : 'servicio'))
              .join(' y ');
            this.messages.add({
              severity: 'success',
              summary: 'Comanda enviada',
              detail: `Las partidas ya aparecen en ${routes}.`,
            });
          });
      },
    });
  }

  protected destinationLabel(item: OrderItem): string {
    if (item.destination === 'PRODUCTION') {
      return 'Cocina';
    }
    if (item.destination === 'SERVICE') {
      return 'Servicio';
    }
    return 'Sin preparación';
  }

  protected backToOrders(): void {
    void this.router.navigate(['/pedidos']);
  }

  private loadAll(): void {
    this.loading.set(true);
    forkJoin({
      detail: this.orderApi.findDetail(this.orderId),
      categories: this.catalogApi.findCategories(),
      products: this.catalogApi.findProducts(),
      modifierGroups: this.catalogApi.findModifierGroups(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ detail, categories, products, modifierGroups }) => {
        this.detail.set(detail);
        this.categories.set(categories);
        this.products.set(products);
        this.modifierGroups.set(modifierGroups);
        this.selectedCategoryId.set(categories.find((category) => category.active)?.id ?? null);
      });
  }

  private modifierError(): string | null {
    for (const group of this.editorGroups()) {
      const count = this.selectedCount(group);
      if (count < group.minSelections || count > group.maxSelections) {
        return `${group.name}: ${this.modifierRule(group).toLocaleLowerCase('es-MX')}.`;
      }
    }
    return null;
  }
}
