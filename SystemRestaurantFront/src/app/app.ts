import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { AuthApiService } from './core/services/auth-api.service';

@Component({
  selector: 'app-root',
  imports: [ButtonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastModule, ToolbarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthApiService);
  private readonly router = inject(Router);

  protected toggleDarkMode(): void {
    document.documentElement.classList.toggle('app-dark');
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigate(['/login']));
  }
}
