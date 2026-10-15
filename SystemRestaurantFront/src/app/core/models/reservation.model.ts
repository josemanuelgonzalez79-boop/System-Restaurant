export type ReservationStatus = 'BOOKED' | 'SEATED' | 'CANCELLED' | 'NO_SHOW';

export interface Reservation {
  id: number;
  branchId: number;
  servicePointId: number;
  customerId: number | null;
  servicePointName: string;
  areaName: string;
  guestName: string;
  guestPhone: string | null;
  guestCount: number;
  notes: string | null;
  startAt: string;
  endAt: string;
  timezone: string;
  status: ReservationStatus;
  orderId: number | null;
  createdByUsername: string;
  updatedByUsername: string;
  cancelReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ReservationSavePayload {
  servicePointId: number;
  customerId?: number | null;
  startLocal: string;
  durationMinutes: number;
  guestName: string;
  guestPhone: string;
  guestCount: number;
  notes: string;
  expectedVersion?: number;
}

export interface ReservationEvent {
  id: number;
  action: 'CREATED' | 'RESCHEDULED' | 'CANCELLED' | 'NO_SHOW' | 'SEATED';
  detail: string;
  actorUsername: string;
  occurredAt: string;
}
