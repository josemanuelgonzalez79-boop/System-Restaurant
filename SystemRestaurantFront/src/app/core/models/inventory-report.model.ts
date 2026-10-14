import { IngredientUnit } from './inventory.model';

export interface InventoryReportIngredient {
  id: number;
  name: string;
  unit: IngredientUnit;
  active: boolean;
  stockQuantity: number;
  minimumQuantity: number;
  unitCost: number | null;
  stockValue: number | null;
  lowStock: boolean;
  outOfStock: boolean;
  purchases: number;
  manualUse: number;
  waste: number;
  adjustments: number;
  orderConsumption: number;
  orderReversal: number;
  netDelta: number;
  knownOrderCost: number;
  unpricedOrderMovements: number;
}

export interface InventoryReport {
  from: string;
  to: string;
  timezone: string;
  generatedAt: string;
  activeIngredients: number;
  lowStock: number;
  outOfStock: number;
  missingCosts: number;
  unvaluedStock: number;
  knownStockValue: number;
  knownOrderCost: number;
  unpricedOrderMovements: number;
  ingredients: InventoryReportIngredient[];
}

export interface InventoryCountLine {
  ingredientId: number;
  ingredientName: string;
  unit: IngredientUnit;
  expectedVersion: number;
  expectedQuantity: number;
  countedQuantity: number;
  difference: number;
  unitCostSnapshot: number | null;
  differenceValue: number | null;
  movementId: number | null;
}

export interface InventoryCount {
  id: number;
  operationId: string;
  reason: string;
  recordedByUsername: string;
  countedAt: string;
  adjustedLines: number;
  knownDifferenceValue: number;
  unpricedDifferences: number;
  lines: InventoryCountLine[];
}

export interface InventoryCountRequest {
  operationId: string;
  reason: string;
  lines: {
    ingredientId: number;
    expectedVersion: number;
    countedQuantity: number;
  }[];
}
