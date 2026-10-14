import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { RecipeSavePayload } from '../models/recipe.model';
import { RecipeApiService } from './recipe-api.service';

describe('RecipeApiService', () => {
  let service: RecipeApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RecipeApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads a recipe for a product', () => {
    service.find('PRODUCT', 7).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/v1/recipes/products/7`);
    expect(request.request.method).toBe('GET');
    request.flush({ id: null, ownerType: 'PRODUCT', ownerId: 7, lines: [] });
  });

  it('sends the previous version and ingredients for a modifier option', () => {
    const payload: RecipeSavePayload = {
      expectedVersion: 4,
      lines: [{ ingredientId: 3, quantity: 0.125 }],
    };

    service.save('MODIFIER_OPTION', 9, payload).subscribe();

    const request = http.expectOne(`${environment.apiBaseUrl}/v1/recipes/modifier-options/9`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 20, ownerType: 'MODIFIER_OPTION', ownerId: 9, version: 5, lines: [] });
  });
});
