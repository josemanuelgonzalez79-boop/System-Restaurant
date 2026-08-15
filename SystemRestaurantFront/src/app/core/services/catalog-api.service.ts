import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Category,
  CategoryPayload,
  ModifierGroup,
  ModifierGroupPayload,
  ModifierOption,
  ModifierOptionPayload,
  Product,
  ProductPayload,
} from '../models/catalog.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);
  private readonly categoriesUrl = `${environment.apiBaseUrl}/v1/categories`;
  private readonly productsUrl = `${environment.apiBaseUrl}/v1/products`;
  private readonly modifierGroupsUrl = `${environment.apiBaseUrl}/v1/modifier-groups`;
  private readonly modifierOptionsUrl = `${environment.apiBaseUrl}/v1/modifier-options`;

  findCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.categoriesUrl);
  }

  createCategory(payload: CategoryPayload): Observable<Category> {
    return this.http.post<Category>(this.categoriesUrl, payload);
  }

  updateCategory(id: number, payload: CategoryPayload): Observable<Category> {
    return this.http.put<Category>(`${this.categoriesUrl}/${id}`, payload);
  }

  changeCategoryActive(id: number, active: boolean): Observable<Category> {
    return this.http.patch<Category>(`${this.categoriesUrl}/${id}/active`, { active });
  }

  findProducts(categoryId?: number): Observable<Product[]> {
    const params = categoryId
      ? new HttpParams().set('categoryId', categoryId.toString())
      : undefined;
    return this.http.get<Product[]>(this.productsUrl, { params });
  }

  createProduct(payload: ProductPayload): Observable<Product> {
    return this.http.post<Product>(this.productsUrl, payload);
  }

  updateProduct(id: number, payload: ProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.productsUrl}/${id}`, payload);
  }

  changeProductActive(id: number, active: boolean): Observable<Product> {
    return this.http.patch<Product>(`${this.productsUrl}/${id}/active`, { active });
  }

  changeProductAvailability(id: number, available: boolean): Observable<Product> {
    return this.http.patch<Product>(`${this.productsUrl}/${id}/availability`, { available });
  }

  findModifierGroups(productId?: number): Observable<ModifierGroup[]> {
    const params = productId ? new HttpParams().set('productId', productId.toString()) : undefined;
    return this.http.get<ModifierGroup[]>(this.modifierGroupsUrl, { params });
  }

  createModifierGroup(payload: ModifierGroupPayload): Observable<ModifierGroup> {
    return this.http.post<ModifierGroup>(this.modifierGroupsUrl, payload);
  }

  updateModifierGroup(id: number, payload: ModifierGroupPayload): Observable<ModifierGroup> {
    return this.http.put<ModifierGroup>(`${this.modifierGroupsUrl}/${id}`, payload);
  }

  changeModifierGroupActive(id: number, active: boolean): Observable<ModifierGroup> {
    return this.http.patch<ModifierGroup>(`${this.modifierGroupsUrl}/${id}/active`, { active });
  }

  createModifierOption(payload: ModifierOptionPayload): Observable<ModifierOption> {
    return this.http.post<ModifierOption>(this.modifierOptionsUrl, payload);
  }

  updateModifierOption(id: number, payload: ModifierOptionPayload): Observable<ModifierOption> {
    return this.http.put<ModifierOption>(`${this.modifierOptionsUrl}/${id}`, payload);
  }

  changeModifierOptionActive(id: number, active: boolean): Observable<ModifierOption> {
    return this.http.patch<ModifierOption>(`${this.modifierOptionsUrl}/${id}/active`, { active });
  }
}
