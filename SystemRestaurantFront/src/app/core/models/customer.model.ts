export interface RestaurantCustomer {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  preferences: string | null;
  dietaryNotes: string | null;
  active: boolean;
  version: number;
  createdByUsername: string;
  updatedByUsername: string;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerSavePayload {
  name: string;
  phone: string;
  email: string;
  preferences: string;
  dietaryNotes: string;
  expectedVersion?: number;
}

export interface CustomerVisit {
  reservationId: number | null;
  orderId: number | null;
  status: string;
  serviceMode: string;
  servicePointName: string | null;
  occurredAt: string;
}

export interface CustomerAudit {
  id: number;
  action: 'CREATED' | 'UPDATED' | 'ARCHIVED' | 'RESTORED';
  actorUsername: string;
  occurredAt: string;
}

export interface CustomerHistory {
  visits: CustomerVisit[];
  changes: CustomerAudit[];
}
