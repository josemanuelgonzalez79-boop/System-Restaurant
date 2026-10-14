import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Ingredient } from '../models/inventory.model';
import { CostChange, CostUpdatePayload, ProductCost } from '../models/cost.model';

@Injectable({ providedIn: 'root' })
export class CostApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/costs`;

  catalog(): Observable<ProductCost[]> {
    return this.http.get<ProductCost[]>(`${this.url}/catalog`);
  }

  history(ingredientId: number): Observable<CostChange[]> {
    return this.http.get<CostChange[]>(`${this.url}/ingredients/${ingredientId}/history`);
  }

  update(ingredientId: number, payload: CostUpdatePayload): Observable<Ingredient> {
    return this.http.put<Ingredient>(`${this.url}/ingredients/${ingredientId}`, payload);
  }
}
