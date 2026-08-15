import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PaymentCreatePayload } from '../models/payment.model';
import { PaymentApiService } from './payment-api.service';

describe('PaymentApiService', () => {
  let service: PaymentApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaymentApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpController.verify());

  it('should register a payment with its operation and order versions', () => {
    const payload: PaymentCreatePayload = {
      operationId: '117e81b6-d15a-42a0-b2bc-4ff36b398e54',
      amount: 100,
      tenderedAmount: 200,
      method: 'CASH',
      methodLabel: '',
      reference: '',
      notes: '',
      orderVersion: 4,
    };

    service.add(12, payload).subscribe();

    const request = httpController.expectOne('/api/v1/orders/12/payments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({});
  });

  it('should send the current version and reason when voiding', () => {
    service.voidPayment(12, 8, 5, 'Captura incorrecta').subscribe();

    const request = httpController.expectOne('/api/v1/orders/12/payments/8/void');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      orderVersion: 5,
      reason: 'Captura incorrecta',
    });
    request.flush({});
  });
});
