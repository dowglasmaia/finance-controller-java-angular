import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/generated/categories';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type Schemas = components['schemas'];

export type Category = Schemas['Category'];
export type Subcategory = Schemas['Subcategory'];
export type CategoryType = Schemas['CategoryType'];
export type CategoryRequest = Schemas['CategoryRequest'];
export type CreateCategoryRequest = Schemas['CreateCategoryRequest'];

/** Cliente do contrato `categories` (sem estado). */
@Injectable({ providedIn: 'root' })
export class CategoriesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listCategories(type?: 'INCOME' | 'EXPENSE'): Observable<Category[]> {
    const params = type ? new HttpParams().set('type', type) : undefined;
    return this.http.get<Category[]>(`${this.baseUrl}/categories`, { params });
  }

  createCategory(request: CreateCategoryRequest): Observable<Category> {
    return this.http.post<Category>(`${this.baseUrl}/categories`, request);
  }

  updateCategory(categoryId: string, request: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/categories/${categoryId}`, request);
  }

  deleteCategory(categoryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${categoryId}`);
  }

  addSubcategory(categoryId: string, name: string): Observable<Subcategory> {
    return this.http.post<Subcategory>(`${this.baseUrl}/categories/${categoryId}/subcategories`, { name });
  }

  deleteSubcategory(categoryId: string, subcategoryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${categoryId}/subcategories/${subcategoryId}`);
  }
}
