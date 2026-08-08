import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import {
  EssentialUser,
  UserCreatePayload,
  UserRole,
  UserUpdatePayload,
} from '../../core/models/user.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { UserApiService } from '../../core/services/user-api.service';

@Component({
  selector: 'app-users',
  imports: [
    ButtonModule,
    CardModule,
    DialogModule,
    InputTextModule,
    PasswordModule,
    ReactiveFormsModule,
    SelectModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly userApi = inject(UserApiService);
  private readonly auth = inject(AuthApiService);
  private readonly messages = inject(MessageService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly users = signal<EssentialUser[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly resetUser = signal<EssentialUser | null>(null);
  protected readonly currentUsername = this.auth.user()?.username;

  protected readonly roles: { label: string; value: UserRole }[] = [
    { label: 'Propietario', value: 'OWNER' },
    { label: 'Administrador', value: 'ADMIN' },
    { label: 'Gerente', value: 'MANAGER' },
    { label: 'Caja', value: 'CASHIER' },
    { label: 'Operador', value: 'OPERATOR' },
  ];

  protected readonly form = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(80),
        Validators.pattern(/^[A-Za-z0-9._-]+$/),
      ],
    ],
    role: this.formBuilder.nonNullable.control<UserRole>('OPERATOR', Validators.required),
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(72)]],
  });

  protected readonly passwordForm = this.formBuilder.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(72)]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const editingId = this.editingId();
    this.saving.set(true);

    const request = editingId
      ? this.userApi.update(editingId, {
          fullName: value.fullName,
          role: value.role,
        } satisfies UserUpdatePayload)
      : this.userApi.create({
          fullName: value.fullName,
          username: value.username,
          role: value.role,
          password: value.password,
        } satisfies UserCreatePayload);

    request.pipe(finalize(() => this.saving.set(false))).subscribe((user) => {
      this.users.update((items) => this.upsert(items, user));
      this.cancelEdit();
      this.messages.add({
        severity: 'success',
        summary: editingId ? 'Usuario actualizado' : 'Usuario creado',
        detail: 'Los cambios quedaron guardados.',
      });
    });
  }

  protected edit(user: EssentialUser): void {
    this.editingId.set(user.id);
    this.form.controls.username.disable();
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
    this.form.setValue({
      fullName: user.fullName,
      username: user.username,
      role: user.role,
      password: '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.form.controls.username.enable();
    this.form.controls.password.setValidators([
      Validators.required,
      Validators.minLength(10),
      Validators.maxLength(72),
    ]);
    this.form.reset({
      fullName: '',
      username: '',
      role: 'OPERATOR',
      password: '',
    });
    this.form.controls.password.updateValueAndValidity();
  }

  protected toggleActive(user: EssentialUser): void {
    this.userApi.changeActive(user.id, !user.active).subscribe((updated) => {
      this.users.update((items) => this.upsert(items, updated));
    });
  }

  protected openPasswordReset(user: EssentialUser): void {
    this.passwordForm.reset();
    this.resetUser.set(user);
  }

  protected closePasswordReset(): void {
    this.resetUser.set(null);
    this.passwordForm.reset();
  }

  protected submitPasswordReset(): void {
    const user = this.resetUser();
    if (!user || this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.userApi.resetPassword(user.id, this.passwordForm.getRawValue().password).subscribe(() => {
      this.closePasswordReset();
      this.messages.add({
        severity: 'success',
        summary: 'Contraseña actualizada',
        detail: `Se cambió la contraseña de ${user.fullName}.`,
      });
    });
  }

  protected roleLabel(role: UserRole): string {
    return this.roles.find((option) => option.value === role)?.label ?? role;
  }

  private load(): void {
    this.loading.set(true);
    this.userApi
      .findAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((users) => this.users.set(users));
  }

  private upsert(items: EssentialUser[], updated: EssentialUser): EssentialUser[] {
    const result = items.some((item) => item.id === updated.id)
      ? items.map((item) => (item.id === updated.id ? updated : item))
      : [...items, updated];
    return result.sort((left, right) => left.fullName.localeCompare(right.fullName));
  }
}
