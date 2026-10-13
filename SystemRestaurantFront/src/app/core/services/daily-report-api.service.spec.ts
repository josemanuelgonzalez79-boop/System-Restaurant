import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DailyReportApiService } from './daily-report-api.service';
import { environment } from '../../../environments/environment';

describe('DailyReportApiService', () => {
  let service: DailyReportApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DailyReportApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpController.verify());

  it('should request the selected branch calendar day', () => {
    service.findDaily(4, '2026-09-14').subscribe();

    const request = httpController.expectOne(
      `${environment.apiBaseUrl}/v1/reports/daily?branchId=4&date=2026-09-14`,
    );
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('should keep date and branch as query parameters', () => {
    service.findDaily(12, '2026-01-02').subscribe();

    const request = httpController.expectOne(
      (candidate) =>
        candidate.url === `${environment.apiBaseUrl}/v1/reports/daily` &&
        candidate.params.get('branchId') === '12' &&
        candidate.params.get('date') === '2026-01-02',
    );
    expect(request.request.body).toBeNull();
    request.flush({});
  });
});
