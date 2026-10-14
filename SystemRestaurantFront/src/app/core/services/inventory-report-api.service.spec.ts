import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { InventoryReportApiService } from './inventory-report-api.service';

describe('InventoryReportApiService', () => {
  let service: InventoryReportApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InventoryReportApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the chosen date range without a branch identifier', () => {
    service.report('2026-10-01', '2026-10-14').subscribe();
    const request = http.expectOne(
      (candidate) => candidate.url === `${environment.apiBaseUrl}/v1/inventory/report`,
    );
    expect(request.request.params.get('from')).toBe('2026-10-01');
    expect(request.request.params.get('to')).toBe('2026-10-14');
    expect(request.request.params.has('branchId')).toBe(false);
    request.flush({ ingredients: [] });
  });

  it('retries a physical count with the same operation identifier', () => {
    const payload = {
      operationId: crypto.randomUUID(),
      reason: 'Conteo semanal',
      lines: [{ ingredientId: 8, expectedVersion: 2, countedQuantity: 0 }],
    };
    service.reconcile(payload).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/v1/inventory/counts`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 7 });
  });
});
