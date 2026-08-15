import { ProductDestination } from './catalog.model';
import { RestaurantOrder } from './order.model';

export type PreparationStatus =
  | 'PENDING'
  | 'IN_PREPARATION'
  | 'READY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface PreparationModifier {
  id: number;
  groupName: string;
  optionName: string;
}

export interface PreparationItem {
  id: number;
  orderItemId: number;
  productName: string;
  quantity: number;
  notes: string | null;
  status: PreparationStatus;
  modifiers: PreparationModifier[];
  version: number;
  startedAt: string | null;
  readyAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PreparationTicket {
  id: number;
  order: RestaurantOrder;
  destination: Exclude<ProductDestination, 'NONE'>;
  sequenceNumber: number;
  sentByUserId: number;
  sentByUserName: string;
  sentAt: string;
  status: PreparationStatus;
  items: PreparationItem[];
}
