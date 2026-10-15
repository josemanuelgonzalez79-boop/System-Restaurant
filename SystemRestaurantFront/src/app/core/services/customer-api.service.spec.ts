import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CustomerApiService } from './customer-api.service';

describe('CustomerApiService', () => {
  let service: CustomerApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CustomerApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches customers locally without sending a branch identifier', () => {
    service.search('667 123', false).subscribe();
    const request = http.expectOne(
      `${environment.apiBaseUrl}/v1/customers?search=667%20123&includeArchived=false`,
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.has('branchId')).toBe(false);
    request.flush([]);
  });

  it('sends expected version when archiving a profile', () => {
    service.changeActive(8, 3, false).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/v1/customers/8/active`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ expectedVersion: 3, active: false });
    request.flush({ id: 8, active: false, version: 4 });
  });
});
