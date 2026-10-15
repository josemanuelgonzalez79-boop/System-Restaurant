export type DiscountType = 'PERCENT' | 'FIXED';

export interface Promotion {
  id: number;
  code: string;
  name: string;
  discountType: DiscountType;
  discountValue: number;
  minimumSubtotal: number;
  startsOn: string;
  endsOn: string;
  active: boolean;
  version: number;
  updatedByUsername: string;
  updatedAt: string;
}

export interface PromotionSavePayload {
  code: string;
  name: string;
  discountType: DiscountType;
  discountValue: number;
  minimumSubtotal: number;
  startsOn: string;
  endsOn: string;
  active: boolean;
  expectedVersion?: number;
}
