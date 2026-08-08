import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OrderCreatePayload } from '../models/order.model';
import { OrderApiService } from './order-api.service';

describe('OrderApiService', () => {
  let service: OrderApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('should find active orders by branch', () => {
    service.findOrders(7).subscribe();

    const request = httpController.expectOne('/api/v1/orders?activeOnly=true&branchId=7');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('should create a takeout order', () => {
    const payload: OrderCreatePayload = {
      branchId: 1,
      servicePointId: null,
      serviceMode: 'TAKEOUT',
      assignedUserId: 4,
      guestCount: 1,
      customerReference: 'Manuel',
      notes: '',
    };

    service.create(payload).subscribe();

    const request = httpController.expectOne('/api/v1/orders');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 1, folio: 'PED-000001', ...payload });
  });

  it('should send the current version when changing status', () => {
    service.changeStatus(9, 'IN_PROGRESS', 3).subscribe();

    const request = httpController.expectOne('/api/v1/orders/9/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'IN_PROGRESS', version: 3 });
    request.flush({});
  });
});
