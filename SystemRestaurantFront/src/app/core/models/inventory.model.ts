export type IngredientUnit = 'PIECE' | 'KG' | 'L';
export type MovementType =
  'PURCHASE' | 'USE' | 'WASTE' | 'ADJUSTMENT' | 'ORDER_CONSUMPTION' | 'ORDER_REVERSAL';
export type ManualMovementType = Exclude<MovementType, 'ORDER_CONSUMPTION' | 'ORDER_REVERSAL'>;

export interface Ingredient {
  id: number;
  name: string;
  unit: IngredientUnit;
  stockQuantity: number;
  minimumQuantity: number;
  lowStock: boolean;
  active: boolean;
  version: number;
  updatedAt: string;
}

export interface IngredientPayload {
  name: string;
  unit: IngredientUnit;
  minimumQuantity: number;
  version?: number;
}

export interface StockMovementPayload {
  operationId: string;
  movementType: ManualMovementType;
  quantity: number;
  reason: string;
  expectedVersion: number;
}

export interface StockMovement {
  id: number;
  operationId: string;
  ingredientId: number;
  preparationItemId: number | null;
  reversesMovementId: number | null;
  movementType: MovementType;
  quantityDelta: number;
  stockAfter: number;
  reason: string;
  recordedByUsername: string;
  occurredAt: string;
}
