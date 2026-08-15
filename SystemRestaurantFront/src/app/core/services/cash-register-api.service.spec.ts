import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CashRegisterApiService } from './cash-register-api.service';

describe('CashRegisterApiService', () => {
  let service: CashRegisterApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CashRegisterApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpController.verify());

  it('should request the open cash register for a branch', () => {
    service.findCurrent(3).subscribe();

    const request = httpController.expectOne('/api/v1/cash-registers/current?branchId=3');
    expect(request.request.method).toBe('GET');
    request.flush(null);
  });

  it('should create an audited cash movement', () => {
    const payload = {
      operationId: '117e81b6-d15a-42a0-b2bc-4ff36b398e54',
      movementType: 'CASH_OUT' as const,
      amount: 75,
      concept: 'Compra de hielo',
      sessionVersion: 4,
    };

    service.addMovement(12, payload).subscribe();

    const request = httpController.expectOne('/api/v1/cash-registers/12/movements');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({});
  });
});
