import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Category, CategoryPayload, Product, ProductPayload } from '../models/catalog.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);
  private readonly categoriesUrl = `${environment.apiBaseUrl}/v1/categories`;
  private readonly productsUrl = `${environment.apiBaseUrl}/v1/products`;

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
}
