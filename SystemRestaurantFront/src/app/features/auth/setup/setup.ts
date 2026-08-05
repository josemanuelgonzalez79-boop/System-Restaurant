import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { InitialSetupPayload } from '../../../core/models/user.model';
import { AuthApiService } from '../../../core/services/auth-api.service';

@Component({
  selector: 'app-setup',
  imports: [ButtonModule, InputTextModule, PasswordModule, ReactiveFormsModule],
  templateUrl: './setup.html',
  styleUrl: '../shared-auth.scss',
})
export class Setup implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  protected readonly loading = signal(false);
  protected readonly form = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    username: [
      'admin',
      [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(80),
        Validators.pattern(/^[A-Za-z0-9._-]+$/),
      ],
    ],
    password: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(72)]],
    confirmPassword: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.auth.ensureInitialized().subscribe(() => {
      if (!this.auth.setupRequired()) {
        void this.router.navigate([this.auth.authenticated() ? '/' : '/login']);
      }
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    if (value.password !== value.confirmPassword) {
      this.messages.add({
        severity: 'warn',
        summary: 'Las contraseñas no coinciden',
        detail: 'Escribe la misma contraseña en ambos campos.',
      });
      return;
    }

    const payload: InitialSetupPayload = {
      fullName: value.fullName,
      username: value.username,
      password: value.password,
    };

    this.loading.set(true);
    this.auth
      .setup(payload)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(() => {
        this.messages.add({
          severity: 'success',
          summary: 'Essential está listo',
          detail: 'Se creó la cuenta propietaria de esta instalación.',
        });
        void this.router.navigate(['/']);
      });
  }
}
