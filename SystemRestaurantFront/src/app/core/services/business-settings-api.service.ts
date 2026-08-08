import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { BusinessSettings, BusinessSettingsPayload } from '../models/business-settings.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BusinessSettingsApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/settings`;

  get(): Observable<BusinessSettings> {
    return this.http.get<BusinessSettings>(this.url);
  }

  update(payload: BusinessSettingsPayload): Observable<BusinessSettings> {
    return this.http.put<BusinessSettings>(this.url, payload);
  }
}
