import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Promotion, PromotionSavePayload } from '../models/promotion.model';

@Injectable({ providedIn: 'root' })
export class PromotionApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/promotions`;

  findAll(availableOnly = false): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(this.url, {
      params: new HttpParams().set('availableOnly', availableOnly),
    });
  }

  create(payload: PromotionSavePayload): Observable<Promotion> {
    return this.http.post<Promotion>(this.url, payload);
  }

  update(id: number, payload: PromotionSavePayload): Observable<Promotion> {
    return this.http.put<Promotion>(`${this.url}/${id}`, payload);
  }
}
