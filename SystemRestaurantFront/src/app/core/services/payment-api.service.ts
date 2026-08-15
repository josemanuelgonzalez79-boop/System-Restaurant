import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PaymentCreatePayload, PaymentSummary } from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  private readonly ordersUrl = `${environment.apiBaseUrl}/v1/orders`;

  findSummary(orderId: number): Observable<PaymentSummary> {
    return this.http.get<PaymentSummary>(`${this.ordersUrl}/${orderId}/payments`);
  }

  add(orderId: number, payload: PaymentCreatePayload): Observable<PaymentSummary> {
    return this.http.post<PaymentSummary>(`${this.ordersUrl}/${orderId}/payments`, payload);
  }

  voidPayment(
    orderId: number,
    paymentId: number,
    orderVersion: number,
    reason: string,
  ): Observable<PaymentSummary> {
    return this.http.patch<PaymentSummary>(
      `${this.ordersUrl}/${orderId}/payments/${paymentId}/void`,
      { orderVersion, reason },
    );
  }

  close(orderId: number, orderVersion: number): Observable<PaymentSummary> {
    return this.http.post<PaymentSummary>(`${this.ordersUrl}/${orderId}/payments/close`, {
      orderVersion,
    });
  }
}
