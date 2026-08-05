export interface Category {
  id: number;
  name: string;
  description: string | null;
  sortOrder: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryPayload {
  name: string;
  description: string;
  sortOrder: number;
}

export type ProductDestination = 'PRODUCTION' | 'SERVICE' | 'NONE';

export interface Product {
  id: number;
  categoryId: number;
  categoryName: string;
  sku: string | null;
  name: string;
  description: string | null;
  price: number;
  destination: ProductDestination;
  active: boolean;
  available: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPayload {
  categoryId: number;
  sku: string;
  name: string;
  description: string;
  price: number;
  destination: ProductDestination;
}
