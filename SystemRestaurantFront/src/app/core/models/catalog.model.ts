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

export interface ModifierOption {
  id: number;
  groupId: number;
  groupName: string;
  productId: number;
  name: string;
  priceDelta: number;
  sortOrder: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ModifierGroup {
  id: number;
  productId: number;
  productName: string;
  name: string;
  minSelections: number;
  maxSelections: number;
  sortOrder: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
  options: ModifierOption[];
}

export interface ModifierGroupPayload {
  productId: number;
  name: string;
  minSelections: number;
  maxSelections: number;
  sortOrder: number;
}

export interface ModifierOptionPayload {
  groupId: number;
  name: string;
  priceDelta: number;
  sortOrder: number;
}
