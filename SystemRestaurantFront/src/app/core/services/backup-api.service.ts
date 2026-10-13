import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BackupApiService {
  private readonly http = inject(HttpClient);

  create(): Observable<HttpResponse<Blob>> {
    return this.http.post(`${environment.apiBaseUrl}/v1/backups`, null, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
