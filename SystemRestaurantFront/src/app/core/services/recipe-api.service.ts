import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Recipe, RecipeOwnerType, RecipeSavePayload } from '../models/recipe.model';

@Injectable({ providedIn: 'root' })
export class RecipeApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/recipes`;

  find(type: RecipeOwnerType, id: number): Observable<Recipe> {
    return this.http.get<Recipe>(this.targetUrl(type, id));
  }

  save(type: RecipeOwnerType, id: number, payload: RecipeSavePayload): Observable<Recipe> {
    return this.http.put<Recipe>(this.targetUrl(type, id), payload);
  }

  private targetUrl(type: RecipeOwnerType, id: number): string {
    return `${this.url}/${type === 'PRODUCT' ? 'products' : 'modifier-options'}/${id}`;
  }
}
