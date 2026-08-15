import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { AuthApiService } from './core/services/auth-api.service';
import { RealtimeConnectionState } from './core/models/realtime.model';
import { RealtimeService } from './core/services/realtime.service';

@Component({
  selector: 'app-root',
  imports: [ButtonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastModule, ToolbarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthApiService);
  protected readonly realtime = inject(RealtimeService);
  private readonly router = inject(Router);

  protected toggleDarkMode(): void {
    document.documentElement.classList.toggle('app-dark');
  }

  protected logout(): void {
    this.realtime.disconnect();
    this.auth.logout().subscribe(() => void this.router.navigate(['/login']));
  }

  protected connectionLabel(state: RealtimeConnectionState): string {
    const labels: Record<RealtimeConnectionState, string> = {
      IDLE: 'Sin canal activo',
      CONNECTING: 'Conectando…',
      CONNECTED: 'Tiempo real',
      RECONNECTING: 'Reconectando…',
      DISCONNECTED: 'Sin conexión',
    };
    return labels[state];
  }
}
