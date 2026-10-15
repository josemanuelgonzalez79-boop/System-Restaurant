import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PaymentSummary } from '../models/payment.model';
import { DiscountType } from '../models/promotion.model';

export interface ApplyDiscountPayload {
  operationId: string;
  orderVersion: number;
  promotionId?: number;
  manualType?: DiscountType;
  manualValue?: number;
  reason?: string;
}

@Injectable({ providedIn: 'root' })
export class DiscountApiService {
  private readonly http = inject(HttpClient);
  private readonly ordersUrl = `${environment.apiBaseUrl}/v1/orders`;

  apply(orderId: number, payload: ApplyDiscountPayload): Observable<PaymentSummary> {
    return this.http.post<PaymentSummary>(`${this.ordersUrl}/${orderId}/discounts`, payload);
  }

  voidDiscount(
    orderId: number,
    discountId: number,
    orderVersion: number,
    reason: string,
  ): Observable<PaymentSummary> {
    return this.http.patch<PaymentSummary>(
      `${this.ordersUrl}/${orderId}/discounts/${discountId}/void`,
      { orderVersion, reason },
    );
  }
}
