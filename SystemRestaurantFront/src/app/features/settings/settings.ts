import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';

import { RestaurantSettingsPayload } from '../../core/models/restaurant-settings.model';
import { RestaurantSettingsApiService } from '../../core/services/restaurant-settings-api.service';

@Component({
  selector: 'app-settings',
  imports: [ButtonModule, CardModule, InputTextModule, ReactiveFormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly settingsApi = inject(RestaurantSettingsApiService);
  private readonly messages = inject(MessageService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly version = signal<number | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    businessName: ['', [Validators.required, Validators.maxLength(120)]],
    displayName: ['', [Validators.required, Validators.maxLength(80)]],
    currencyCode: ['MXN', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    timezone: ['America/Mazatlan', [Validators.required, Validators.maxLength(60)]],
    primaryColor: ['#2563EB', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    secondaryColor: ['#111827', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    phone: ['', Validators.maxLength(30)],
    address: ['', Validators.maxLength(250)],
  });

  ngOnInit(): void {
    this.load();
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const payload: RestaurantSettingsPayload = this.form.getRawValue();
    this.settingsApi
      .update(payload)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe((settings) => {
        this.version.set(settings.version);
        this.messages.add({
          severity: 'success',
          summary: 'Configuración guardada',
          detail: 'Los datos del restaurante se actualizaron correctamente.',
        });
      });
  }

  private load(): void {
    this.loading.set(true);
    this.settingsApi
      .get()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((settings) => {
        this.version.set(settings.version);
        this.form.setValue({
          businessName: settings.businessName,
          displayName: settings.displayName,
          currencyCode: settings.currencyCode,
          timezone: settings.timezone,
          primaryColor: settings.primaryColor,
          secondaryColor: settings.secondaryColor,
          phone: settings.phone ?? '',
          address: settings.address ?? '',
        });
      });
  }
}
