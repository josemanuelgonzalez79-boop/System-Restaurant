import { OrderDetail } from './order.model';

export type PaymentMethod = 'CASH' | 'CARD' | 'TRANSFER' | 'OTHER';
export type PaymentStatus = 'ACTIVE' | 'VOIDED';

export interface OrderPayment {
  id: number;
  folio: string;
  operationId: string;
  cashRegisterSessionId: number | null;
  amount: number;
  tenderedAmount: number;
  changeAmount: number;
  method: PaymentMethod;
  methodLabel: string | null;
  reference: string | null;
  notes: string | null;
  status: PaymentStatus;
  receivedByUserId: number;
  receivedByUserName: string;
  receivedAt: string;
  voidedByUserId: number | null;
  voidedByUserName: string | null;
  voidedAt: string | null;
  voidReason: string | null;
}

export interface OrderDiscount {
  id: number;
  source: 'PROMOTION' | 'MANUAL';
  code: string | null;
  name: string;
  discountType: 'PERCENT' | 'FIXED';
  discountValue: number;
  baseSubtotal: number;
  amount: number;
  reason: string | null;
  status: 'ACTIVE' | 'VOIDED';
  appliedByUserName: string;
  appliedAt: string;
  voidedByUserName: string | null;
  voidedAt: string | null;
  voidReason: string | null;
}

export interface PaymentSummary {
  orderDetail: OrderDetail;
  payments: OrderPayment[];
  discounts: OrderDiscount[];
  subtotal: number;
  discountAmount: number;
  total: number;
  paid: number;
  balance: number;
  settled: boolean;
  canClose: boolean;
  closeBlockingReason: string | null;
  openCashRegisterSessionId: number | null;
  openCashRegisterSessionFolio: string | null;
}

export interface PaymentCreatePayload {
  operationId: string;
  amount: number;
  tenderedAmount: number;
  method: PaymentMethod;
  methodLabel: string;
  reference: string;
  notes: string;
  orderVersion: number;
}
