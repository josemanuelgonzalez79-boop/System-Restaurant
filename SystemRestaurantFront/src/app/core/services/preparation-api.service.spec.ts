import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PreparationApiService } from './preparation-api.service';

describe('PreparationApiService', () => {
  let service: PreparationApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PreparationApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('should find active kitchen tickets by branch', () => {
    service.findTickets(3, 'PRODUCTION').subscribe();

    const request = httpController.expectOne(
      '/api/v1/preparation?branchId=3&activeOnly=true&destination=PRODUCTION',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('should send item status with its optimistic version', () => {
    service.changeItemStatus(18, 'IN_PREPARATION', 2).subscribe();

    const request = httpController.expectOne('/api/v1/preparation/items/18/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'IN_PREPARATION', version: 2 });
    request.flush({});
  });
});
