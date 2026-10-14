import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CostApiService } from './cost-api.service';

describe('CostApiService', () => {
  let service: CostApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CostApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the catalog cost summary', () => {
    service.catalog().subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/v1/costs/catalog`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('sends the version and reason when changing a reference cost', () => {
    const payload = { unitCost: 87.5, expectedVersion: 3, reason: 'Nueva factura' };
    service.update(7, payload).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/v1/costs/ingredients/7`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 7, unitCost: 87.5, version: 4 });
  });
});
