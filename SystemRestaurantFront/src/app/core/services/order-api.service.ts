import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  OrderCreatePayload,
  OrderOperator,
  OrderStatus,
  RestaurantOrder,
} from '../models/order.model';
import { Branch } from '../models/structure.model';

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly http = inject(HttpClient);
  private readonly ordersUrl = `${environment.apiBaseUrl}/v1/orders`;

  findBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.ordersUrl}/branches`);
  }

  findOrders(branchId?: number, activeOnly = true): Observable<RestaurantOrder[]> {
    let params = new HttpParams().set('activeOnly', activeOnly.toString());
    if (branchId !== undefined) {
      params = params.set('branchId', branchId.toString());
    }
    return this.http.get<RestaurantOrder[]>(this.ordersUrl, { params });
  }

  findOperators(branchId: number): Observable<OrderOperator[]> {
    const params = new HttpParams().set('branchId', branchId.toString());
    return this.http.get<OrderOperator[]>(`${this.ordersUrl}/operators`, { params });
  }

  create(payload: OrderCreatePayload): Observable<RestaurantOrder> {
    return this.http.post<RestaurantOrder>(this.ordersUrl, payload);
  }

  changeStatus(id: number, status: OrderStatus, version: number): Observable<RestaurantOrder> {
    return this.http.patch<RestaurantOrder>(`${this.ordersUrl}/${id}/status`, {
      status,
      version,
    });
  }
}
