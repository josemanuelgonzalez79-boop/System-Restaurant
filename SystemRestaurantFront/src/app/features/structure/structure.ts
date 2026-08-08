import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
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
  BranchAssignmentUser,
  BranchPayload,
  OperationalArea,
  OperationalAreaPayload,
  ServicePoint,
  ServicePointPayload,
  ServicePointType,
} from '../../core/models/structure.model';
import { UserRole } from '../../core/models/user.model';
import { StructureApiService } from '../../core/services/structure-api.service';

@Component({
  selector: 'app-structure',
  imports: [
    ButtonModule,
    CardModule,
    FormsModule,
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
  protected readonly savingBranch = signal(false);
  protected readonly savingArea = signal(false);
  protected readonly savingPoint = signal(false);
  protected readonly loadingAssignments = signal(false);
  protected readonly savingAssignments = signal(false);

  protected readonly branches = signal<Branch[]>([]);
  protected readonly areas = signal<OperationalArea[]>([]);
  protected readonly points = signal<ServicePoint[]>([]);
  protected readonly assignmentUsers = signal<BranchAssignmentUser[]>([]);
  protected readonly assignedUserIds = signal<ReadonlySet<number>>(new Set());
  protected readonly assignmentBranchId = signal<number | null>(null);

  protected readonly editingBranchId = signal<number | null>(null);
  protected readonly editingAreaId = signal<number | null>(null);
  protected readonly editingPointId = signal<number | null>(null);

  protected readonly activeBranches = computed(() =>
    this.branches().filter((branch) => branch.active),
  );
  protected readonly activeAreas = computed(() => {
    const activeBranchIds = new Set(this.activeBranches().map((branch) => branch.id));
    return this.areas().filter((area) => area.active && activeBranchIds.has(area.branchId));
  });
  protected readonly hasAssignedOwner = computed(() => {
    const assignedIds = this.assignedUserIds();
    return this.assignmentUsers().some(
      (user) => user.active && user.role === 'OWNER' && assignedIds.has(user.userId),
    );
  });

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
    { label: 'Habitación o consultorio', value: 'ROOM' },
    { label: 'Escritorio', value: 'DESK' },
    { label: 'Ventanilla', value: 'WINDOW' },
    { label: 'Otro', value: 'OTHER' },
  ];

  protected readonly branchForm = this.formBuilder.nonNullable.group({
    code: [
      '',
      [Validators.required, Validators.maxLength(30), Validators.pattern(/^[A-Za-z0-9_-]+$/)],
    ],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    address: ['', Validators.maxLength(250)],
    phone: ['', Validators.maxLength(30)],
    timezone: ['America/Mazatlan', [Validators.required, Validators.maxLength(60)]],
    sortOrder: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly areaForm = this.formBuilder.group({
    branchId: this.formBuilder.control<number | null>(null, Validators.required),
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(120),
    ]),
    description: this.formBuilder.nonNullable.control('', Validators.maxLength(250)),
    areaType: this.formBuilder.nonNullable.control<AreaType>('SERVICE', Validators.required),
    sortOrder: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(0)]),
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

  protected submitBranch(): void {
    if (this.branchForm.invalid) {
      this.branchForm.markAllAsTouched();
      return;
    }

    const payload = this.branchForm.getRawValue() satisfies BranchPayload;
    const editingId = this.editingBranchId();
    const request = editingId
      ? this.structureApi.updateBranch(editingId, payload)
      : this.structureApi.createBranch(payload);

    this.savingBranch.set(true);
    request.pipe(finalize(() => this.savingBranch.set(false))).subscribe((branch) => {
      this.branches.update((items) => this.upsertBranch(items, branch));
      this.refreshBranchNames(branch);
      this.ensureSelections(branch.id);
      this.cancelBranchEdit();
      this.showSuccess(editingId ? 'Sucursal actualizada' : 'Sucursal creada');
    });
  }

  protected editBranch(branch: Branch): void {
    this.editingBranchId.set(branch.id);
    this.branchForm.setValue({
      code: branch.code,
      name: branch.name,
      address: branch.address ?? '',
      phone: branch.phone ?? '',
      timezone: branch.timezone,
      sortOrder: branch.sortOrder,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelBranchEdit(): void {
    this.editingBranchId.set(null);
    this.branchForm.reset({
      code: '',
      name: '',
      address: '',
      phone: '',
      timezone: 'America/Mazatlan',
      sortOrder: 0,
    });
  }

  protected toggleBranch(branch: Branch): void {
    this.structureApi.changeBranchActive(branch.id, !branch.active).subscribe((updated) => {
      this.branches.update((items) => this.upsertBranch(items, updated));
      this.ensureSelections();
    });
  }

  protected submitArea(): void {
    if (this.areaForm.invalid) {
      this.areaForm.markAllAsTouched();
      return;
    }

    const value = this.areaForm.getRawValue();
    const payload: OperationalAreaPayload = {
      branchId: value.branchId as number,
      name: value.name,
      description: value.description,
      areaType: value.areaType,
      sortOrder: value.sortOrder,
    };
    const editingId = this.editingAreaId();
    const request = editingId
      ? this.structureApi.updateArea(editingId, payload)
      : this.structureApi.createArea(payload);

    this.savingArea.set(true);
    request.pipe(finalize(() => this.savingArea.set(false))).subscribe((area) => {
      this.areas.update((items) => this.upsertArea(items, area));
      this.refreshAreaNames(area);
      this.ensureSelections(undefined, area.id);
      this.cancelAreaEdit();
      this.showSuccess(editingId ? 'Área actualizada' : 'Área creada');
    });
  }

  protected editArea(area: OperationalArea): void {
    this.editingAreaId.set(area.id);
    this.areaForm.setValue({
      branchId: area.branchId,
      name: area.name,
      description: area.description ?? '',
      areaType: area.areaType,
      sortOrder: area.sortOrder,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelAreaEdit(): void {
    this.editingAreaId.set(null);
    this.areaForm.reset({
      branchId: this.activeBranches()[0]?.id ?? null,
      name: '',
      description: '',
      areaType: 'SERVICE',
      sortOrder: 0,
    });
  }

  protected toggleArea(area: OperationalArea): void {
    this.structureApi.changeAreaActive(area.id, !area.active).subscribe((updated) => {
      this.areas.update((items) => this.upsertArea(items, updated));
      this.ensureSelections();
    });
  }

  protected submitPoint(): void {
    if (this.pointForm.invalid) {
      this.pointForm.markAllAsTouched();
      return;
    }

    const value = this.pointForm.getRawValue();
    const payload: ServicePointPayload = {
      areaId: value.areaId as number,
      code: value.code,
      name: value.name,
      description: value.description,
      pointType: value.pointType,
      sortOrder: value.sortOrder,
    };
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

  protected changeAssignmentBranch(branchId: number | null): void {
    this.assignmentBranchId.set(branchId);
    this.assignmentUsers.set([]);
    this.assignedUserIds.set(new Set());
    if (branchId !== null) {
      this.loadAssignments(branchId);
    }
  }

  protected toggleAssignment(userId: number, assigned: boolean): void {
    const next = new Set(this.assignedUserIds());
    if (assigned) {
      next.add(userId);
    } else {
      next.delete(userId);
    }
    this.assignedUserIds.set(next);
  }

  protected saveAssignments(): void {
    const branchId = this.assignmentBranchId();
    if (branchId === null || !this.hasAssignedOwner()) {
      return;
    }

    this.savingAssignments.set(true);
    this.structureApi
      .replaceAssignments(branchId, [...this.assignedUserIds()])
      .pipe(finalize(() => this.savingAssignments.set(false)))
      .subscribe((users) => {
        this.setAssignmentUsers(users);
        this.showSuccess('Asignaciones actualizadas');
      });
  }

  protected areaTypeLabel(type: AreaType): string {
    return this.areaTypes.find((option) => option.value === type)?.label ?? type;
  }

  protected pointTypeLabel(type: ServicePointType): string {
    return this.pointTypes.find((option) => option.value === type)?.label ?? type;
  }

  protected roleLabel(role: UserRole): string {
    const labels: Record<UserRole, string> = {
      OWNER: 'Propietario',
      ADMIN: 'Administrador',
      MANAGER: 'Gerente',
      CASHIER: 'Caja',
      OPERATOR: 'Operador',
    };
    return labels[role];
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
        this.branches.set(branches);
        this.areas.set(areas);
        this.points.set(points);
        this.ensureSelections();
      });
  }

  private ensureSelections(preferredBranchId?: number, preferredAreaId?: number): void {
    const currentAreaBranch = this.areaForm.controls.branchId.value;
    if (!this.activeBranches().some((branch) => branch.id === currentAreaBranch)) {
      this.areaForm.controls.branchId.setValue(
        preferredBranchId ?? this.activeBranches()[0]?.id ?? null,
      );
    }

    const currentPointArea = this.pointForm.controls.areaId.value;
    if (!this.activeAreas().some((area) => area.id === currentPointArea)) {
      this.pointForm.controls.areaId.setValue(preferredAreaId ?? this.activeAreas()[0]?.id ?? null);
    }

    const currentAssignmentBranch = this.assignmentBranchId();
    if (!this.activeBranches().some((branch) => branch.id === currentAssignmentBranch)) {
      const branchId = preferredBranchId ?? this.activeBranches()[0]?.id ?? null;
      this.changeAssignmentBranch(branchId);
    } else if (currentAssignmentBranch !== null && this.assignmentUsers().length === 0) {
      this.loadAssignments(currentAssignmentBranch);
    }
  }

  private loadAssignments(branchId: number): void {
    this.loadingAssignments.set(true);
    this.structureApi
      .findAssignments(branchId)
      .pipe(finalize(() => this.loadingAssignments.set(false)))
      .subscribe((users) => this.setAssignmentUsers(users));
  }

  private setAssignmentUsers(users: BranchAssignmentUser[]): void {
    this.assignmentUsers.set(users);
    this.assignedUserIds.set(
      new Set(users.filter((user) => user.active && user.assigned).map((user) => user.userId)),
    );
  }

  private upsertBranch(items: Branch[], updated: Branch): Branch[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort(
      (left, right) => left.sortOrder - right.sortOrder || left.name.localeCompare(right.name),
    );
  }

  private upsertArea(items: OperationalArea[], updated: OperationalArea): OperationalArea[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort(
      (left, right) =>
        left.branchName.localeCompare(right.branchName) ||
        left.sortOrder - right.sortOrder ||
        left.name.localeCompare(right.name),
    );
  }

  private upsertPoint(items: ServicePoint[], updated: ServicePoint): ServicePoint[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort(
      (left, right) =>
        left.branchName.localeCompare(right.branchName) ||
        left.areaName.localeCompare(right.areaName) ||
        left.sortOrder - right.sortOrder ||
        left.name.localeCompare(right.name),
    );
  }

  private refreshBranchNames(branch: Branch): void {
    this.areas.update((items) =>
      items.map((area) =>
        area.branchId === branch.id ? { ...area, branchName: branch.name } : area,
      ),
    );
    this.points.update((items) =>
      items.map((point) =>
        point.branchId === branch.id ? { ...point, branchName: branch.name } : point,
      ),
    );
  }

  private refreshAreaNames(area: OperationalArea): void {
    this.points.update((items) =>
      items.map((point) =>
        point.areaId === area.id
          ? {
              ...point,
              areaName: area.name,
              branchId: area.branchId,
              branchName: area.branchName,
            }
          : point,
      ),
    );
  }

  private showSuccess(summary: string): void {
    this.messages.add({
      severity: 'success',
      summary,
      detail: 'Los cambios quedaron guardados.',
    });
  }
}
