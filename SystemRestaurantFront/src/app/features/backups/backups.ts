import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';

import { BackupApiService } from '../../core/services/backup-api.service';

@Component({
  selector: 'app-backups',
  imports: [ButtonModule],
  templateUrl: './backups.html',
  styleUrl: './backups.scss',
})
export class BackupsPage {
  private readonly api = inject(BackupApiService);
  protected readonly working = signal(false);
  protected readonly lastDownload = signal<string | null>(null);
  protected readonly failureMessage = signal<string | null>(null);

  protected download(): void {
    if (this.working()) return;
    this.failureMessage.set(null);
    this.working.set(true);
    this.api
      .create()
      .pipe(finalize(() => this.working.set(false)))
      .subscribe({
        next: (response) => {
          if (!response.body || response.body.size === 0) {
            this.failureMessage.set('El servidor devolvió un respaldo vacío. Revisa PostgreSQL.');
            return;
          }
          const filename =
            /filename="?([^";]+)"?/i.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ??
            `Essential-${new Date().toISOString().slice(0, 10)}.zip`;
          const url = URL.createObjectURL(response.body);
          const link = document.createElement('a');
          link.href = url;
          link.download = filename;
          document.body.appendChild(link);
          link.click();
          link.remove();
          setTimeout(() => URL.revokeObjectURL(url), 60_000);
          this.lastDownload.set(filename);
        },
        error: (error: unknown) => void this.showFailure(error),
      });
  }

  private async showFailure(error: unknown): Promise<void> {
    const fallback =
      'No se pudo generar el respaldo. Comprueba pg_dump y pg_restore en el servidor.';
    if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
      try {
        const problem: unknown = JSON.parse(await error.error.text());
        if (problem && typeof problem === 'object' && 'detail' in problem) {
          const detail = problem.detail;
          if (typeof detail === 'string' && detail.trim()) {
            this.failureMessage.set(detail);
            return;
          }
        }
      } catch {
        // Puede ser una respuesta de un proxy; se muestra el mensaje de respaldo.
      }
    }
    this.failureMessage.set(fallback);
  }
}
