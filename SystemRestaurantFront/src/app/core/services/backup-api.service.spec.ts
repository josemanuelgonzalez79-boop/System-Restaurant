import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { BackupApiService } from './backup-api.service';

describe('BackupApiService', () => {
  let service: BackupApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BackupApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpController.verify());

  it('requests an owner-only binary download via the same-origin API', () => {
    service.create().subscribe((response) => {
      expect(response.body?.size).toBe(3);
      expect(response.headers.get('Content-Disposition')).toContain('Essential-');
    });
    const request = httpController.expectOne(`${environment.apiBaseUrl}/v1/backups`);
    expect(request.request.method).toBe('POST');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['abc']), {
      headers: { 'Content-Disposition': 'attachment; filename="Essential-test.zip"' },
    });
  });
});
