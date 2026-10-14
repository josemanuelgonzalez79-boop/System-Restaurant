import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { auditTime, finalize, forkJoin, interval, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

import { IngredientUnit } from '../../core/models/inventory.model';
import { InventoryCount, InventoryReport } from '../../core/models/inventory-report.model';
import { InventoryApiService } from '../../core/services/inventory-api.service';
import { InventoryReportApiService } from '../../core/services/inventory-report-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { StructureApiService } from '../../core/services/structure-api.service';

interface CountDraft {
  ingredientId: number;
  name: string;
  unit: IngredientUnit;
  expectedQuantity: number;
  expectedVersion: number;
  countedQuantity: number | null;
}

@Component({
  selector: 'app-inventory-report',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    ButtonModule,
    InputNumberModule,
    InputTextModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './inventory-report.html',
  styleUrl: './inventory-report.scss',
})
export class InventoryReportPage implements OnInit, OnDestroy {
  private readonly api = inject(InventoryReportApiService);
  private readonly inventory = inject(InventoryApiService);
  private readonly orders = inject(OrderApiService);
  private readonly structure = inject(StructureApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly messages = inject(MessageService);
  private branchLookup?: Subscription;
  private realtimeSubscription?: Subscription;
  private fallbackSubscription?: Subscription;
  private generation = 0;
  private pendingOperationId: string | null = null;

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly startingCount = signal(false);
  protected readonly report = signal<InventoryReport | null>(null);
  protected readonly counts = signal<InventoryCount[]>([]);
  protected readonly selectedCount = signal<InventoryCount | null>(null);
  protected readonly draft = signal<CountDraft[] | null>(null);
  protected readonly staleCount = signal(false);
  protected readonly reportError = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly alertOnly = signal(false);
  protected readonly visible = computed(() => {
    const term = this.search().trim().toLocaleLowerCase('es');
    return (this.report()?.ingredients ?? []).filter(
      (item) =>
        (!this.alertOnly() ||
          (item.active && (item.lowStock || item.outOfStock || item.unitCost === null))) &&
        (!term || item.name.toLocaleLowerCase('es').includes(term)),
    );
  });
  protected readonly countedLines = computed(
    () => this.draft()?.filter((line) => line.countedQuantity !== null).length ?? 0,
  );
  protected readonly changedLines = computed(
    () =>
      this.draft()?.filter(
        (line) => line.countedQuantity !== null && line.countedQuantity !== line.expectedQuantity,
      ).length ?? 0,
  );

  protected from = this.dateInZone(new Date(), 'UTC');
  protected to = this.from;
  protected maximumDate = this.to;
  protected reason = '';

  ngOnInit(): void {
    this.branchLookup = forkJoin({
      all: this.structure.findBranches(),
      assigned: this.orders.findBranches(),
    }).subscribe({
      next: ({ all, assigned }) => {
        const branch = all[0];
        if (branch) {
          this.maximumDate = this.dateInZone(new Date(), branch.timezone);
          this.to = this.maximumDate;
          const start = new Date();
          start.setUTCDate(start.getUTCDate() - 6);
          this.from = this.dateInZone(start, branch.timezone);
          if (assigned.some((item) => item.id === branch.id)) {
            this.realtimeSubscription = this.realtime
              .watchBranch(branch.id)
              .pipe(auditTime(350))
              .subscribe(() => this.refresh());
          }
        }
        this.refresh();
      },
      error: () => {
        this.reportError.set('No se pudo consultar la configuración del restaurante.');
        this.loading.set(false);
      },
    });
    this.fallbackSubscription = interval(60_000).subscribe(() => {
      if (!this.saving()) this.refresh();
    });
    this.refreshCounts();
  }

  ngOnDestroy(): void {
    this.branchLookup?.unsubscribe();
    this.realtimeSubscription?.unsubscribe();
    this.fallbackSubscription?.unsubscribe();
  }

  protected refresh(): void {
    if (!this.validRange()) return;
    const generation = ++this.generation;
    this.loading.set(true);
    this.api
      .report(this.from, this.to)
      .pipe(
        finalize(() => {
          if (this.generation === generation) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (report) => {
          if (this.generation !== generation) return;
          this.report.set(report);
          this.reportError.set(null);
        },
        error: () => {
          if (this.generation === generation) {
            this.reportError.set('No se pudo cargar el reporte. Intenta actualizar.');
          }
        },
      });
  }

  protected validRange(): boolean {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(this.from) || !/^\d{4}-\d{2}-\d{2}$/.test(this.to))
      return false;
    const days =
      (Date.parse(`${this.to}T00:00:00Z`) - Date.parse(`${this.from}T00:00:00Z`)) / 86400000;
    return Number.isInteger(days) && days >= 0 && days <= 30 && this.to <= this.maximumDate;
  }

  protected startCount(): void {
    if (this.startingCount() || this.saving()) return;
    this.startingCount.set(true);
    this.inventory
      .findAll()
      .pipe(finalize(() => this.startingCount.set(false)))
      .subscribe({
        next: (ingredients) => {
          const active = ingredients.filter((ingredient) => ingredient.active);
          if (active.length === 0) {
            this.messages.add({ severity: 'warn', summary: 'Primero registra insumos activos' });
            return;
          }
          this.draft.set(
            active.map((item) => ({
              ingredientId: item.id,
              name: item.name,
              unit: item.unit,
              expectedQuantity: item.stockQuantity,
              expectedVersion: item.version,
              countedQuantity: null,
            })),
          );
          this.reason = '';
          this.staleCount.set(false);
          this.pendingOperationId = null;
        },
        error: () => {},
      });
  }

  protected cancelCount(): void {
    this.draft.set(null);
    this.staleCount.set(false);
    this.pendingOperationId = null;
  }

  protected updateCount(id: number, value: number | null): void {
    this.draft.update(
      (lines) =>
        lines?.map((line) =>
          line.ingredientId === id ? { ...line, countedQuantity: value } : line,
        ) ?? null,
    );
    this.pendingOperationId = null;
  }

  protected reasonChanged(): void {
    this.pendingOperationId = null;
  }

  protected submitCount(): void {
    const lines = this.draft();
    if (!lines || this.saving() || this.staleCount()) return;
    if (
      this.reason.trim().length < 5 ||
      this.reason.trim().length > 250 ||
      lines.some(
        (line) =>
          line.countedQuantity === null ||
          !Number.isFinite(line.countedQuantity) ||
          line.countedQuantity < 0 ||
          line.countedQuantity > 99999999999.999 ||
          Math.abs(Math.round(line.countedQuantity * 1000) - line.countedQuantity * 1000) >
            0.0001 ||
          (line.unit === 'PIECE' && !Number.isInteger(line.countedQuantity)),
      )
    ) {
      this.messages.add({
        severity: 'warn',
        summary: 'Completa todas las cantidades y el motivo (mínimo 5 caracteres)',
      });
      return;
    }
    this.saving.set(true);
    this.pendingOperationId ??= crypto.randomUUID();
    this.api
      .reconcile({
        operationId: this.pendingOperationId,
        reason: this.reason.trim(),
        lines: lines.map((line) => ({
          ingredientId: line.ingredientId,
          expectedVersion: line.expectedVersion,
          countedQuantity: line.countedQuantity!,
        })),
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (count) => {
          this.messages.add({
            severity: 'success',
            summary: `Conteo #${count.id} conciliado`,
            detail: `${count.adjustedLines} insumos ajustados`,
          });
          this.selectedCount.set(count);
          this.draft.set(null);
          this.pendingOperationId = null;
          this.refresh();
          this.refreshCounts();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 409) this.staleCount.set(true);
        },
      });
  }

  protected refreshCounts(): void {
    this.api.counts().subscribe({
      next: (counts) => {
        this.counts.set(counts);
        if (this.selectedCount() === null && counts.length) this.selectedCount.set(counts[0]);
      },
      error: () => {},
    });
  }

  protected unit(unit: IngredientUnit): string {
    return unit === 'PIECE' ? 'pzas' : unit === 'KG' ? 'kg' : 'L';
  }

  protected downloadCsv(): void {
    const report = this.report();
    if (!report) return;
    const headers = [
      'Insumo',
      'Unidad',
      'Activo',
      'Existencia actual',
      'Mínimo actual',
      'Costo unitario',
      'Valor actual',
      'Compras',
      'Salida manual',
      'Merma',
      'Ajustes',
      'Consumo comandas',
      'Reversas',
      'Variación neta',
      'Costo conocido de comandas',
      'Movimientos sin costo',
    ];
    const rows = report.ingredients.map((item) => [
      item.name,
      this.unit(item.unit),
      item.active ? 'Sí' : 'No',
      item.stockQuantity,
      item.minimumQuantity,
      item.unitCost ?? '',
      item.stockValue ?? '',
      item.purchases,
      item.manualUse,
      item.waste,
      item.adjustments,
      item.orderConsumption,
      item.orderReversal,
      item.netDelta,
      item.knownOrderCost,
      item.unpricedOrderMovements,
    ]);
    this.saveCsv(`inventario-${report.from}-a-${report.to}.csv`, [
      ['Periodo', report.from, report.to, 'Zona', report.timezone],
      ['Existencias y valor son actuales al generar; movimientos corresponden al periodo.'],
      headers,
      ...rows,
    ]);
  }

  protected downloadCountCsv(count: InventoryCount): void {
    this.saveCsv(`conteo-${count.id}.csv`, [
      ['Conteo', count.id, 'Fecha', count.countedAt, 'Responsable', count.recordedByUsername],
      ['Motivo', count.reason],
      [
        'Insumo',
        'Unidad',
        'Esperado',
        'Contado',
        'Diferencia',
        'Costo de referencia',
        'Valor diferencia',
        'Movimiento',
      ],
      ...count.lines.map((item) => [
        item.ingredientName,
        this.unit(item.unit),
        item.expectedQuantity,
        item.countedQuantity,
        item.difference,
        item.unitCostSnapshot ?? '',
        item.differenceValue ?? '',
        item.movementId ?? '',
      ]),
    ]);
  }

  private saveCsv(filename: string, rows: (string | number)[][]): void {
    const cell = (value: string | number): string => {
      const raw = String(value);
      const safe = typeof value === 'string' && /^[\s]*[=+\-@]/.test(raw) ? `'${raw}` : raw;
      return `"${safe.replaceAll('"', '""')}"`;
    };
    const blob = new Blob(['\uFEFF', rows.map((row) => row.map(cell).join(',')).join('\r\n')], {
      type: 'text/csv;charset=utf-8',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  private dateInZone(date: Date, zone: string): string {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: zone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(date);
    const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
    return `${get('year')}-${get('month')}-${get('day')}`;
  }
}
