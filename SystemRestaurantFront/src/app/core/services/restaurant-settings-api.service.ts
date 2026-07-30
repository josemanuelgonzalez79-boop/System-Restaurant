import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { RestaurantSettings, RestaurantSettingsPayload } from '../models/restaurant-settings.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RestaurantSettingsApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/settings`;

  get(): Observable<RestaurantSettings> {
    return this.http.get<RestaurantSettings>(this.url);
  }

  update(payload: RestaurantSettingsPayload): Observable<RestaurantSettings> {
    return this.http.put<RestaurantSettings>(this.url, payload);
  }
}
