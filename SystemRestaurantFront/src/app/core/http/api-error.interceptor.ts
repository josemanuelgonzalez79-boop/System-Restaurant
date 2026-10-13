import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const messages = inject(MessageService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const validationErrors = error.error?.errors;
      const firstValidationError =
        validationErrors && typeof validationErrors === 'object'
          ? Object.values(validationErrors).find(
              (value): value is string => typeof value === 'string',
            )
          : undefined;
      const detail =
        firstValidationError ?? error.error?.detail ?? error.error?.message ?? error.message;

      const isSessionProbe = request.url.endsWith('/auth/me') && error.status === 401;
      const isBackupDownload = request.url.endsWith('/v1/backups') && request.method === 'POST';
      if (!isSessionProbe && !isBackupDownload) {
        messages.add({
          severity: 'error',
          summary: 'No fue posible completar la solicitud',
          detail: typeof detail === 'string' ? detail : 'Ocurrió un error inesperado.',
        });
      }

      return throwError(() => error);
    }),
  );
};
