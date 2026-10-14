import { IngredientUnit } from './inventory.model';

export interface CostLine {
  ingredientId: number;
  ingredientName: string;
  unit: IngredientUnit;
  quantity: number;
  unitCost: number | null;
  lineCost: number | null;
}

export interface RecipeCost {
  configured: boolean;
  cost: number | null;
  missingCosts: string[];
  lines: CostLine[];
}

export interface OptionCost {
  id: number;
  groupName: string;
  name: string;
  priceDelta: number;
  active: boolean;
  recipe: RecipeCost;
  grossProfit: number | null;
  marginPercent: number | null;
}

export interface ProductCost {
  id: number;
  name: string;
  categoryName: string;
  price: number;
  destination: 'PRODUCTION' | 'SERVICE' | 'NONE';
  active: boolean;
  recipe: RecipeCost;
  grossProfit: number | null;
  marginPercent: number | null;
  options: OptionCost[];
}

export interface CostChange {
  id: number;
  ingredientId: number;
  previousUnitCost: number | null;
  newUnitCost: number;
  reason: string;
  changedByUsername: string;
  changedAt: string;
}

export interface CostUpdatePayload {
  unitCost: number;
  expectedVersion: number;
  reason: string;
}
