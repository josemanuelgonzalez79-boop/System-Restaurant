import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DiscountApiService } from './discount-api.service';

describe('DiscountApiService', () => {
  let service: DiscountApiService;
  let requests: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DiscountApiService);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requests.verify());

  it('sends operation and order versions so a second cashier cannot replay a stale discount', () => {
    const payload = {
      operationId: '5132362b-c41f-46ac-a0a8-fdd80eaad998',
      orderVersion: 6,
      promotionId: 3,
    };
    service.apply(42, payload).subscribe();
    const request = requests.expectOne(`${environment.apiBaseUrl}/v1/orders/42/discounts`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({});
  });

  it('requires a reason and the latest order version to void a discount', () => {
    service.voidDiscount(42, 8, 7, 'Promoción equivocada').subscribe();
    const request = requests.expectOne(`${environment.apiBaseUrl}/v1/orders/42/discounts/8/void`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ orderVersion: 7, reason: 'Promoción equivocada' });
    request.flush({});
  });
});
