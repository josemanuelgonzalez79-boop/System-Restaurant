import { IngredientUnit } from './inventory.model';

export type RecipeOwnerType = 'PRODUCT' | 'MODIFIER_OPTION';

export interface RecipeLine {
  ingredientId: number;
  ingredientName: string;
  unit: IngredientUnit;
  ingredientActive: boolean;
  quantity: number;
  stockQuantity: number;
}

export interface Recipe {
  id: number | null;
  ownerType: RecipeOwnerType;
  ownerId: number;
  ownerName: string;
  version: number | null;
  updatedByUsername: string | null;
  updatedAt: string | null;
  lines: RecipeLine[];
}

export interface RecipeSavePayload {
  expectedVersion: number | null;
  lines: { ingredientId: number; quantity: number }[];
}
