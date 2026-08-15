export type CashRegisterStatus = 'OPEN' | 'CLOSED';
export type CashMovementType = 'CASH_IN' | 'CASH_OUT';
export type CashMovementStatus = 'ACTIVE' | 'VOIDED';

export interface CashRegisterSession {
  id: number;
  folio: string;
  branchId: number;
  branchName: string;
  openingAmount: number;
  openingNotes: string | null;
  status: CashRegisterStatus;
  openedByUserId: number;
  openedByUserName: string;
  openedAt: string;
  closedByUserId: number | null;
  closedByUserName: string | null;
  closedAt: string | null;
  expectedCashAtClose: number | null;
  countedCashAtClose: number | null;
  differenceAtClose: number | null;
  closingNotes: string | null;
  version: number;
  updatedAt: string;
}

export interface CashMovement {
  id: number;
  folio: string;
  operationId: string;
  movementType: CashMovementType;
  amount: number;
  concept: string;
  status: CashMovementStatus;
  recordedByUserId: number;
  recordedByUserName: string;
  recordedAt: string;
  voidedByUserId: number | null;
  voidedByUserName: string | null;
  voidedAt: string | null;
  voidReason: string | null;
}

export interface CashRegisterSummary {
  session: CashRegisterSession;
  movements: CashMovement[];
  cashPayments: number;
  cardPayments: number;
  transferPayments: number;
  otherPayments: number;
  cashIn: number;
  cashOut: number;
  expectedCash: number;
  canClose: boolean;
  closeBlockingReason: string | null;
}

export interface CashRegisterOpenPayload {
  branchId: number;
  openingAmount: number;
  notes: string;
}

export interface CashMovementCreatePayload {
  operationId: string;
  movementType: CashMovementType;
  amount: number;
  concept: string;
  sessionVersion: number;
}
