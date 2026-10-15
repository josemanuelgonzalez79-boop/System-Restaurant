import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CustomerHistory, CustomerSavePayload, RestaurantCustomer } from '../models/customer.model';

@Injectable({ providedIn: 'root' })
export class CustomerApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/customers`;

  search(search = '', includeArchived = false): Observable<RestaurantCustomer[]> {
    const params = new HttpParams().set('search', search).set('includeArchived', includeArchived);
    return this.http.get<RestaurantCustomer[]>(this.url, { params });
  }

  history(id: number): Observable<CustomerHistory> {
    return this.http.get<CustomerHistory>(`${this.url}/${id}/history`);
  }

  find(id: number): Observable<RestaurantCustomer> {
    return this.http.get<RestaurantCustomer>(`${this.url}/${id}`);
  }

  create(payload: CustomerSavePayload): Observable<RestaurantCustomer> {
    return this.http.post<RestaurantCustomer>(this.url, payload);
  }

  update(id: number, payload: CustomerSavePayload): Observable<RestaurantCustomer> {
    return this.http.put<RestaurantCustomer>(`${this.url}/${id}`, payload);
  }

  changeActive(id: number, version: number, active: boolean): Observable<RestaurantCustomer> {
    return this.http.patch<RestaurantCustomer>(`${this.url}/${id}/active`, {
      expectedVersion: version,
      active,
    });
  }
}
