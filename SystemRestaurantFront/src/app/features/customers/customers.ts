import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { finalize } from 'rxjs';

import {
  CustomerHistory,
  CustomerVisit,
  RestaurantCustomer,
} from '../../core/models/customer.model';
import { CustomerApiService } from '../../core/services/customer-api.service';

@Component({
  selector: 'app-customers',
  imports: [
    ButtonModule,
    ConfirmDialogModule,
    DatePipe,
    DialogModule,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
    TagModule,
    TextareaModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './customers.html',
  styleUrl: './customers.scss',
})
export class CustomersPage implements OnInit {
  private readonly api = inject(CustomerApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);
  private readonly route = inject(ActivatedRoute);
  private requestId = 0;

  protected readonly customers = signal<RestaurantCustomer[]>([]);
  protected readonly selected = signal<RestaurantCustomer | null>(null);
  protected readonly history = signal<CustomerHistory | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly includeArchived = signal(false);
  protected readonly form = this.formBuilder.group({
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(120),
    ]),
    phone: this.formBuilder.nonNullable.control('', [
      Validators.maxLength(30),
      Validators.pattern(/^(?:[+() .-]*\d){7,15}[+() .-]*$/),
    ]),
    email: this.formBuilder.nonNullable.control('', [Validators.email, Validators.maxLength(120)]),
    preferences: this.formBuilder.nonNullable.control('', Validators.maxLength(500)),
    dietaryNotes: this.formBuilder.nonNullable.control('', Validators.maxLength(300)),
  });
  protected editingId: number | null = null;
  private editingVersion: number | null = null;

  ngOnInit(): void {
    this.load();
    const id = Number(this.route.snapshot.queryParamMap.get('id'));
    if (Number.isSafeInteger(id) && id > 0) {
      this.api.find(id).subscribe({ next: (customer) => this.select(customer), error: () => {} });
    }
  }

  protected load(): void {
    const id = ++this.requestId;
    this.loading.set(true);
    this.api
      .search(this.searchTerm(), this.includeArchived())
      .pipe(
        finalize(() => {
          if (id === this.requestId) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (items) => {
          if (id !== this.requestId) return;
          this.customers.set(items);
          this.selected.set(
            items.find((item) => item.id === this.selected()?.id) ?? this.selected(),
          );
        },
        error: () => {},
      });
  }

  protected setSearch(value: string): void {
    this.searchTerm.set(value);
  }

  protected toggleArchived(checked: boolean): void {
    this.includeArchived.set(checked);
    this.load();
  }

  protected select(customer: RestaurantCustomer): void {
    this.selected.set(customer);
    this.history.set(null);
    this.loadHistory(customer.id);
  }

  protected newCustomer(): void {
    this.editingId = null;
    this.editingVersion = null;
    this.form.reset({ name: '', phone: '', email: '', preferences: '', dietaryNotes: '' });
    this.dialogVisible.set(true);
  }

  protected edit(customer: RestaurantCustomer): void {
    this.editingId = customer.id;
    this.editingVersion = customer.version;
    this.form.reset({
      name: customer.name,
      phone: customer.phone ?? '',
      email: customer.email ?? '',
      preferences: customer.preferences ?? '',
      dietaryNotes: customer.dietaryNotes ?? '',
    });
    this.dialogVisible.set(true);
  }

  protected save(): void {
    if (this.saving() || this.form.invalid || !this.form.controls.name.value.trim()) {
      this.form.markAllAsTouched();
      this.messages.add({ severity: 'warn', summary: 'Revisa el nombre y el correo del cliente' });
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      ...value,
      name: value.name.trim(),
      ...(this.editingId !== null ? { expectedVersion: this.editingVersion! } : {}),
    };
    this.saving.set(true);
    const request =
      this.editingId === null ? this.api.create(payload) : this.api.update(this.editingId, payload);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (customer) => {
        this.dialogVisible.set(false);
        this.selected.set(customer);
        this.loadHistory(customer.id);
        this.load();
        this.messages.add({ severity: 'success', summary: 'Ficha guardada' });
      },
      error: () => this.load(),
    });
  }

  protected changeActive(customer: RestaurantCustomer): void {
    this.confirmations.confirm({
      header: customer.active ? 'Archivar ficha' : 'Reactivar ficha',
      message: customer.active
        ? `¿Archivar a ${customer.name}? Se conservará su historial, pero no podrá vincularse a cuentas nuevas.`
        : `¿Volver a activar la ficha de ${customer.name}?`,
      acceptLabel: customer.active ? 'Archivar' : 'Reactivar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.saving.set(true);
        this.api
          .changeActive(customer.id, customer.version, !customer.active)
          .pipe(finalize(() => this.saving.set(false)))
          .subscribe({
            next: (updated) => {
              this.selected.set(updated);
              this.loadHistory(updated.id);
              this.load();
              this.messages.add({ severity: 'success', summary: 'Estado actualizado' });
            },
            error: () => this.load(),
          });
      },
    });
  }

  protected visitLabel(visit: CustomerVisit): string {
    if (visit.status === 'BOOKED') return 'Reservada';
    if (visit.status === 'SEATED') return 'Cliente sentado';
    if (visit.status === 'NO_SHOW') return 'No asistió';
    if (visit.status === 'CANCELLED') return 'Cancelada';
    if (visit.status === 'COMPLETED') return 'Pedido completado';
    if (visit.status === 'IN_PROGRESS') return 'En atención';
    return visit.status === 'OPEN' ? 'Pedido abierto' : 'Pedido cancelado';
  }

  protected eventLabel(action: string): string {
    return (
      (
        {
          CREATED: 'Ficha creada',
          UPDATED: 'Ficha editada',
          ARCHIVED: 'Archivada',
          RESTORED: 'Reactivada',
        } as Record<string, string>
      )[action] ?? action
    );
  }

  private loadHistory(id: number): void {
    this.api.history(id).subscribe({
      next: (value) => {
        if (this.selected()?.id === id) this.history.set(value);
      },
      error: () => {},
    });
  }
}
