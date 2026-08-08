import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Category, ProductPayload } from '../models/catalog.model';
import { CatalogApiService } from './catalog-api.service';

describe('CatalogApiService', () => {
  let service: CatalogApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CatalogApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('should request all categories', () => {
    const response: Category[] = [
      {
        id: 1,
        name: 'Hamburguesas',
        description: null,
        sortOrder: 0,
        active: true,
        version: 0,
        createdAt: '2026-07-29T00:00:00Z',
        updatedAt: '2026-07-29T00:00:00Z',
      },
    ];

    service.findCategories().subscribe((categories) => {
      expect(categories).toEqual(response);
    });

    const request = httpController.expectOne('/api/v1/categories');
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('should send the product payload to the API', () => {
    const payload: ProductPayload = {
      categoryId: 1,
      sku: 'HAM-001',
      name: 'Hamburguesa clásica',
      description: '',
      price: 129,
      destination: 'PRODUCTION',
    };

    service.createProduct(payload).subscribe();

    const request = httpController.expectOne('/api/v1/products');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      id: 1,
      categoryName: 'Hamburguesas',
      active: true,
      available: true,
      version: 0,
      createdAt: '2026-07-29T00:00:00Z',
      updatedAt: '2026-07-29T00:00:00Z',
      ...payload,
    });
  });
});
