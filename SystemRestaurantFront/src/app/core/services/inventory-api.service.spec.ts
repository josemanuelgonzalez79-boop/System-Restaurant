import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { StockMovementPayload } from '../models/inventory.model';
import { environment } from '../../../environments/environment';
import { InventoryApiService } from './inventory-api.service';

describe('InventoryApiService', () => {
  let service: InventoryApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InventoryApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the same operation ID and stock version used to prevent duplicate tablet entries', () => {
    const payload: StockMovementPayload = {
      operationId: 'adba5916-7f13-47f5-9602-95705857899d',
      movementType: 'WASTE',
      quantity: 0.25,
      reason: 'Producto dañado',
      expectedVersion: 3,
    };

    service.recordMovement(8, payload).subscribe();

    const request = http.expectOne(
      `${environment.apiBaseUrl}/v1/inventory/ingredients/8/movements`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 2, ingredientId: 8, quantityDelta: -0.25, stockAfter: 4.75 });
  });

  it('reads inventory from the single local restaurant without a branch parameter', () => {
    service.findAll().subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/v1/inventory/ingredients`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
