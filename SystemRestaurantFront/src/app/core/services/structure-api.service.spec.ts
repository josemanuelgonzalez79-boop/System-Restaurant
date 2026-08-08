import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { BranchPayload, OperationalAreaPayload } from '../models/structure.model';
import { StructureApiService } from './structure-api.service';

describe('StructureApiService', () => {
  let service: StructureApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StructureApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('should create a branch', () => {
    const payload: BranchPayload = {
      code: 'CENTRO',
      name: 'Sucursal Centro',
      address: '',
      phone: '',
      timezone: 'America/Mazatlan',
      sortOrder: 0,
    };

    service.createBranch(payload).subscribe();

    const request = httpController.expectOne('/api/v1/branches');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      id: 1,
      active: true,
      version: 0,
      createdAt: '2026-08-08T00:00:00Z',
      updatedAt: '2026-08-08T00:00:00Z',
      ...payload,
    });
  });

  it('should filter areas by branch', () => {
    service.findAreas(7).subscribe();

    const request = httpController.expectOne('/api/v1/areas?branchId=7');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('should send an area payload', () => {
    const payload: OperationalAreaPayload = {
      branchId: 1,
      name: 'Producción',
      description: '',
      areaType: 'PRODUCTION',
      sortOrder: 1,
    };

    service.createArea(payload).subscribe();

    const request = httpController.expectOne('/api/v1/areas');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      id: 1,
      branchName: 'Principal',
      active: true,
      version: 0,
      createdAt: '2026-08-08T00:00:00Z',
      updatedAt: '2026-08-08T00:00:00Z',
      ...payload,
    });
  });

  it('should replace branch assignments', () => {
    service.replaceAssignments(3, [1, 2]).subscribe();

    const request = httpController.expectOne('/api/v1/branches/3/assignments');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ userIds: [1, 2] });
    request.flush([]);
  });
});
