import { UserRole } from './user.model';

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
