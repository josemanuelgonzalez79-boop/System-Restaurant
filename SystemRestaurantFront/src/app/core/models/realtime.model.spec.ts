import { isRealtimeEvent } from './realtime.model';

describe('RealtimeEvent', () => {
  it('should accept a valid branch event', () => {
    expect(
      isRealtimeEvent({
        eventId: '45f9fd17-52a0-4fa0-bf40-0d921e8d9e16',
        type: 'PREPARATION_DISPATCHED',
        branchId: 3,
        orderId: 9,
        ticketId: null,
        preparationItemId: null,
        occurredAt: '2026-08-15T18:00:00Z',
      }),
    ).toBe(true);
  });

  it('should reject an unknown event type', () => {
    expect(
      isRealtimeEvent({
        eventId: 'event',
        type: 'CLIENT_FAKE_EVENT',
        branchId: 3,
        occurredAt: '2026-08-15T18:00:00Z',
      }),
    ).toBe(false);
  });

  it('should accept a cash register event without an order', () => {
    expect(
      isRealtimeEvent({
        eventId: 'e1ef06f3-0292-476b-a811-56cf9f73778c',
        type: 'CASH_REGISTER_CHANGED',
        branchId: 3,
        orderId: null,
        ticketId: null,
        preparationItemId: null,
        occurredAt: '2026-08-15T18:00:00Z',
      }),
    ).toBe(true);
  });
});
