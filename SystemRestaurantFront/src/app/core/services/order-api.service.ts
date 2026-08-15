import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  OrderCreatePayload,
  OrderDetail,
  OrderDispatchResponse,
  OrderItemCreatePayload,
  OrderItemUpdatePayload,
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

  findDetail(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${this.ordersUrl}/${id}/detail`);
  }

  addItem(orderId: number, payload: OrderItemCreatePayload): Observable<OrderDetail> {
    return this.http.post<OrderDetail>(`${this.ordersUrl}/${orderId}/items`, payload);
  }

  updateItem(
    orderId: number,
    itemId: number,
    payload: OrderItemUpdatePayload,
  ): Observable<OrderDetail> {
    return this.http.put<OrderDetail>(`${this.ordersUrl}/${orderId}/items/${itemId}`, payload);
  }

  removeItem(orderId: number, itemId: number, orderVersion: number): Observable<OrderDetail> {
    const params = new HttpParams().set('orderVersion', orderVersion.toString());
    return this.http.delete<OrderDetail>(`${this.ordersUrl}/${orderId}/items/${itemId}`, {
      params,
    });
  }

  dispatch(orderId: number, orderVersion: number): Observable<OrderDispatchResponse> {
    return this.http.post<OrderDispatchResponse>(`${this.ordersUrl}/${orderId}/dispatch`, {
      orderVersion,
    });
  }
}
