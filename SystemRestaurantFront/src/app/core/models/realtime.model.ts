export type RealtimeEventType =
  | 'ORDER_CREATED'
  | 'ORDER_UPDATED'
  | 'ORDER_ITEMS_CHANGED'
  | 'PREPARATION_DISPATCHED'
  | 'PREPARATION_ITEM_CHANGED';

export type RealtimeConnectionState =
  'IDLE' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';

export interface RealtimeEvent {
  eventId: string;
  type: RealtimeEventType;
  branchId: number;
  orderId: number | null;
  ticketId: number | null;
  preparationItemId: number | null;
  occurredAt: string;
}

const eventTypes = new Set<RealtimeEventType>([
  'ORDER_CREATED',
  'ORDER_UPDATED',
  'ORDER_ITEMS_CHANGED',
  'PREPARATION_DISPATCHED',
  'PREPARATION_ITEM_CHANGED',
]);

export function isRealtimeEvent(value: unknown): value is RealtimeEvent {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const candidate = value as Partial<RealtimeEvent>;
  return (
    typeof candidate.eventId === 'string' &&
    typeof candidate.type === 'string' &&
    eventTypes.has(candidate.type as RealtimeEventType) &&
    typeof candidate.branchId === 'number' &&
    typeof candidate.occurredAt === 'string'
  );
}
