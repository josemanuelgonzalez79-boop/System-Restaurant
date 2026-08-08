import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

import { HealthStatus } from '../../core/models/health-status.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { HealthApiService } from '../../core/services/health-api.service';

@Component({
  selector: 'app-home',
  imports: [ButtonModule, CardModule, DatePipe, ProgressSpinnerModule, RouterLink, TagModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly healthApi = inject(HealthApiService);
  protected readonly auth = inject(AuthApiService);

  protected readonly loading = signal(false);
  protected readonly health = signal<HealthStatus | null>(null);

  protected checkBackend(): void {
    this.loading.set(true);

    this.healthApi
      .getHealth()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (value) => this.health.set(value),
      });
  }
}
