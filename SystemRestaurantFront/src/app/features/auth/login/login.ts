import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { AuthApiService } from '../../../core/services/auth-api.service';

@Component({
  selector: 'app-login',
  imports: [ButtonModule, InputTextModule, PasswordModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: '../shared-auth.scss',
})
export class Login implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthApiService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(80)]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });

  ngOnInit(): void {
    this.auth.ensureInitialized().subscribe(() => {
      if (this.auth.setupRequired()) {
        void this.router.navigate(['/setup']);
      } else if (this.auth.authenticated()) {
        void this.router.navigate(['/']);
      }
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth
      .login(username, password)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(() => void this.router.navigate(['/']));
  }
}
