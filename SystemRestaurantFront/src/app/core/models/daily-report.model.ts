import { PaymentMethod } from './payment.model';

export interface DailySalesSummary {
  grossCollected: number;
  voidedAmount: number;
  netCollected: number;
  receivedPaymentCount: number;
  voidedPaymentCount: number;
}

export interface DailyOrderSummary {
  openedCount: number;
  completedCount: number;
  cancelledCount: number;
  pendingAtEndCount: number;
  dineInCount: number;
  takeoutCount: number;
  guestCount: number;
  completedOrderValue: number;
  averageTicket: number;
}

export interface DailyPaymentMethodSummary {
  method: PaymentMethod;
  grossAmount: number;
  voidedAmount: number;
  netAmount: number;
  receivedCount: number;
  voidedCount: number;
}

export interface HourlySalesSummary {
  hour: number;
  netAmount: number;
  receivedCount: number;
  voidedCount: number;
}

export interface ProductSalesSummary {
  rank: number;
  productName: string;
  quantity: number;
  amount: number;
}

export interface CashSessionDailySummary {
  id: number;
  folio: string;
  status: 'OPEN' | 'CLOSED';
  openedByUserName: string;
  openedAt: string;
  closedByUserName: string | null;
  closedAt: string | null;
  openingAmount: number;
  expectedCashAtClose: number | null;
  countedCashAtClose: number | null;
  differenceAtClose: number | null;
}

export interface DailyCashSummary {
  openedSessionCount: number;
  closedSessionCount: number;
  openAtEndCount: number;
  openingAmount: number;
  cashPayments: number;
  cashInGross: number;
  cashInVoided: number;
  cashInNet: number;
  cashOutGross: number;
  cashOutVoided: number;
  cashOutNet: number;
  expectedCashFlow: number;
  closingDifference: number;
  sessions: CashSessionDailySummary[];
}

export interface CancelledOrderDailySummary {
  orderId: number;
  folio: string;
  serviceMode: 'DINE_IN' | 'TAKEOUT';
  customerReference: string | null;
  assignedUserName: string;
  cancelledAt: string;
  amount: number;
}

export interface VoidedPaymentDailySummary {
  paymentId: number;
  folio: string;
  orderId: number;
  orderFolio: string;
  method: PaymentMethod;
  amount: number;
  voidedAt: string;
  voidedByUserName: string;
  reason: string;
}

export interface DailyReport {
  branchId: number;
  branchName: string;
  timezone: string;
  currencyCode: string;
  businessDate: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  sales: DailySalesSummary;
  orders: DailyOrderSummary;
  paymentMethods: DailyPaymentMethodSummary[];
  hourlySales: HourlySalesSummary[];
  topProducts: ProductSalesSummary[];
  cash: DailyCashSummary;
  cancelledOrders: CancelledOrderDailySummary[];
  voidedPayments: VoidedPaymentDailySummary[];
}
