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
  AreaType,
  Branch,
  OperationalArea,
  OperationalAreaPayload,
  ServicePoint,
  ServicePointPayload,
  ServicePointType,
} from '../../core/models/structure.model';
import { StructureApiService } from '../../core/services/structure-api.service';

@Component({
  selector: 'app-structure',
  imports: [
    ButtonModule,
    CardModule,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    SelectModule,
    TableModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './structure.html',
  styleUrl: './structure.scss',
})
export class Structure implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly structureApi = inject(StructureApiService);
  private readonly messages = inject(MessageService);

  protected readonly loading = signal(true);
  protected readonly savingArea = signal(false);
  protected readonly savingPoint = signal(false);
  protected readonly restaurant = signal<Branch | null>(null);
  protected readonly areas = signal<OperationalArea[]>([]);
  protected readonly points = signal<ServicePoint[]>([]);
  protected readonly editingAreaId = signal<number | null>(null);
  protected readonly editingPointId = signal<number | null>(null);
  protected readonly activeAreas = computed(() => this.areas().filter((area) => area.active));

  protected readonly areaTypes: { label: string; value: AreaType }[] = [
    { label: 'Servicio o atención', value: 'SERVICE' },
    { label: 'Producción o preparación', value: 'PRODUCTION' },
    { label: 'Almacén', value: 'STORAGE' },
    { label: 'Caja', value: 'CHECKOUT' },
    { label: 'Oficina', value: 'OFFICE' },
    { label: 'Otra', value: 'OTHER' },
  ];

  protected readonly pointTypes: { label: string; value: ServicePointType }[] = [
    { label: 'Mesa', value: 'TABLE' },
    { label: 'Mostrador', value: 'COUNTER' },
    { label: 'Caja', value: 'CHECKOUT' },
    { label: 'Estación', value: 'STATION' },
    { label: 'Otro', value: 'OTHER' },
  ];

  protected readonly areaForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', Validators.maxLength(250)],
    areaType: ['SERVICE' as AreaType, Validators.required],
    sortOrder: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly pointForm = this.formBuilder.group({
    areaId: this.formBuilder.control<number | null>(null, Validators.required),
    code: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(40),
      Validators.pattern(/^[A-Za-z0-9_-]+$/),
    ]),
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(120),
    ]),
    description: this.formBuilder.nonNullable.control('', Validators.maxLength(250)),
    pointType: this.formBuilder.nonNullable.control<ServicePointType>('TABLE', Validators.required),
    sortOrder: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(0)]),
  });

  ngOnInit(): void {
    this.load();
  }

  protected submitArea(): void {
    const branchId = this.restaurant()?.id;
    if (!branchId || this.areaForm.invalid) {
      this.areaForm.markAllAsTouched();
      return;
    }
    const payload: OperationalAreaPayload = { ...this.areaForm.getRawValue(), branchId };
    const editingId = this.editingAreaId();
    const request = editingId
      ? this.structureApi.updateArea(editingId, payload)
      : this.structureApi.createArea(payload);

    this.savingArea.set(true);
    request.pipe(finalize(() => this.savingArea.set(false))).subscribe((area) => {
      this.areas.update((items) => this.upsertArea(items, area));
      this.points.update((items) =>
        items.map((point) =>
          point.areaId === area.id ? { ...point, areaName: area.name } : point,
        ),
      );
      this.ensurePointArea(area.id);
      this.cancelAreaEdit();
      this.showSuccess(editingId ? 'Área actualizada' : 'Área creada');
    });
  }

  protected editArea(area: OperationalArea): void {
    this.editingAreaId.set(area.id);
    this.areaForm.setValue({
      name: area.name,
      description: area.description ?? '',
      areaType: area.areaType,
      sortOrder: area.sortOrder,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelAreaEdit(): void {
    this.editingAreaId.set(null);
    this.areaForm.reset({ name: '', description: '', areaType: 'SERVICE', sortOrder: 0 });
  }

  protected toggleArea(area: OperationalArea): void {
    this.structureApi.changeAreaActive(area.id, !area.active).subscribe((updated) => {
      this.areas.update((items) => this.upsertArea(items, updated));
      this.ensurePointArea();
    });
  }

  protected submitPoint(): void {
    if (this.pointForm.invalid) {
      this.pointForm.markAllAsTouched();
      return;
    }
    const value = this.pointForm.getRawValue();
    const payload: ServicePointPayload = { ...value, areaId: value.areaId as number };
    const editingId = this.editingPointId();
    const request = editingId
      ? this.structureApi.updatePoint(editingId, payload)
      : this.structureApi.createPoint(payload);

    this.savingPoint.set(true);
    request.pipe(finalize(() => this.savingPoint.set(false))).subscribe((point) => {
      this.points.update((items) => this.upsertPoint(items, point));
      this.cancelPointEdit();
      this.showSuccess(editingId ? 'Punto actualizado' : 'Punto creado');
    });
  }

  protected editPoint(point: ServicePoint): void {
    this.editingPointId.set(point.id);
    this.pointForm.setValue({
      areaId: point.areaId,
      code: point.code,
      name: point.name,
      description: point.description ?? '',
      pointType: point.pointType,
      sortOrder: point.sortOrder,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelPointEdit(): void {
    this.editingPointId.set(null);
    this.pointForm.reset({
      areaId: this.activeAreas()[0]?.id ?? null,
      code: '',
      name: '',
      description: '',
      pointType: 'TABLE',
      sortOrder: 0,
    });
  }

  protected togglePoint(point: ServicePoint): void {
    this.structureApi.changePointActive(point.id, !point.active).subscribe((updated) => {
      this.points.update((items) => this.upsertPoint(items, updated));
    });
  }

  protected areaTypeLabel(type: AreaType): string {
    return this.areaTypes.find((option) => option.value === type)?.label ?? type;
  }

  protected pointTypeLabel(type: ServicePointType): string {
    return this.pointTypes.find((option) => option.value === type)?.label ?? type;
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      branches: this.structureApi.findBranches(),
      areas: this.structureApi.findAreas(),
      points: this.structureApi.findPoints(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ branches, areas, points }) => {
        this.restaurant.set(branches[0] ?? null);
        this.areas.set(areas);
        this.points.set(points);
        this.ensurePointArea();
      });
  }

  private ensurePointArea(preferredAreaId?: number): void {
    if (!this.activeAreas().some((area) => area.id === this.pointForm.controls.areaId.value)) {
      this.pointForm.controls.areaId.setValue(preferredAreaId ?? this.activeAreas()[0]?.id ?? null);
    }
  }

  private upsertArea(items: OperationalArea[], updated: OperationalArea): OperationalArea[] {
    return [...items.filter((item) => item.id !== updated.id), updated].sort(
      (a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name),
    );
  }

  private upsertPoint(items: ServicePoint[], updated: ServicePoint): ServicePoint[] {
    return [...items.filter((item) => item.id !== updated.id), updated].sort(
      (a, b) =>
        a.areaName.localeCompare(b.areaName) ||
        a.sortOrder - b.sortOrder ||
        a.name.localeCompare(b.name),
    );
  }

  private showSuccess(summary: string): void {
    this.messages.add({ severity: 'success', summary, detail: 'Los cambios quedaron guardados.' });
  }
}
