import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Ingredient,
  IngredientPayload,
  StockMovement,
  StockMovementPayload,
} from '../models/inventory.model';

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/inventory/ingredients`;

  findAll(): Observable<Ingredient[]> {
    return this.http.get<Ingredient[]>(this.url);
  }

  create(payload: IngredientPayload): Observable<Ingredient> {
    return this.http.post<Ingredient>(this.url, payload);
  }

  update(id: number, payload: IngredientPayload): Observable<Ingredient> {
    return this.http.put<Ingredient>(`${this.url}/${id}`, payload);
  }

  changeActive(id: number, active: boolean): Observable<Ingredient> {
    return this.http.patch<Ingredient>(`${this.url}/${id}/active`, { active });
  }

  findMovements(id: number): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(`${this.url}/${id}/movements`);
  }

  recordMovement(id: number, payload: StockMovementPayload): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.url}/${id}/movements`, payload);
  }
}
