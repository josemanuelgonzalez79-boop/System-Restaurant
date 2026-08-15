import { UserRole } from './user.model';
import { ProductDestination } from './catalog.model';

export type OrderStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type ServiceMode = 'DINE_IN' | 'TAKEOUT';

export interface RestaurantOrder {
  id: number;
  folio: string;
  branchId: number;
  branchName: string;
  servicePointId: number | null;
  servicePointName: string | null;
  areaId: number | null;
  areaName: string | null;
  serviceMode: ServiceMode;
  assignedUserId: number;
  assignedUserName: string;
  openedByUserId: number;
  openedByUserName: string;
  guestCount: number;
  customerReference: string | null;
  notes: string | null;
  status: OrderStatus;
  version: number;
  openedAt: string;
  updatedAt: string;
  closedAt: string | null;
}

export interface OrderCreatePayload {
  branchId: number;
  servicePointId: number | null;
  serviceMode: ServiceMode;
  assignedUserId: number;
  guestCount: number;
  customerReference: string;
  notes: string;
}

export interface OrderOperator {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
}

export interface OrderItemModifier {
  id: number;
  modifierOptionId: number;
  groupName: string;
  optionName: string;
  priceDelta: number;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  unitPrice: number;
  destination: ProductDestination;
  quantity: number;
  notes: string | null;
  modifiers: OrderItemModifier[];
  baseSubtotal: number;
  modifierSubtotal: number;
  lineTotal: number;
  sentAt: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderDetail {
  order: RestaurantOrder;
  items: OrderItem[];
  totalUnits: number;
  productSubtotal: number;
  modifierSubtotal: number;
  total: number;
}

export interface OrderItemCreatePayload {
  productId: number;
  quantity: number;
  notes: string;
  modifierOptionIds: number[];
  orderVersion: number;
}

export interface OrderItemUpdatePayload {
  quantity: number;
  notes: string;
  modifierOptionIds: number[];
  orderVersion: number;
}

export interface OrderDispatchResponse {
  orderDetail: OrderDetail;
  tickets: import('./preparation.model').PreparationTicket[];
}
