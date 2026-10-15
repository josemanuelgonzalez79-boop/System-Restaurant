import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { RealtimeEvent } from '../../core/models/realtime.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { CashRegisterApiService } from '../../core/services/cash-register-api.service';
import { DailyReportApiService } from '../../core/services/daily-report-api.service';
import { InventoryReportApiService } from '../../core/services/inventory-report-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { PreparationApiService } from '../../core/services/preparation-api.service';
import { PromotionApiService } from '../../core/services/promotion-api.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { ReservationApiService } from '../../core/services/reservation-api.service';
import { Home } from './home';

describe('Inicio Pro', () => {
  const events = new Subject<RealtimeEvent>();
  const ordersApi = {
    findBranches: vi.fn(() =>
      of([{ id: 1, name: 'Restaurante principal', timezone: 'America/Mazatlan' }]),
    ),
    findOrders: vi.fn(() =>
      of([
        {
          id: 4,
          folio: 'PED-000004',
          branchId: 1,
          serviceMode: 'DINE_IN',
          servicePointId: 7,
          servicePointName: 'Mesa 7',
          assignedUserName: 'Ana',
          openedAt: '2026-10-15T12:00:00',
        },
        {
          id: 5,
          folio: 'PED-000005',
          branchId: 1,
          serviceMode: 'TAKEOUT',
          servicePointId: null,
          servicePointName: null,
          customerReference: 'Carlos',
          assignedUserName: 'Ana',
          openedAt: '2026-10-15T12:05:00',
        },
      ]),
    ),
  };
  const preparationApi = {
    findTickets: vi.fn(() =>
      of([
        {
          items: [{ status: 'PENDING' }, { status: 'IN_PREPARATION' }, { status: 'READY' }],
        },
      ]),
    ),
  };
  const cashApi = { findCurrent: vi.fn(() => of(null)) };
  const inventoryApi = {
    report: vi.fn(() =>
      of({
        activeIngredients: 3,
        outOfStock: 1,
        missingCosts: 1,
        unvaluedStock: 1,
        knownStockValue: 450,
        ingredients: [
          {
            id: 9,
            name: 'Tomate',
            active: true,
            stockQuantity: 0,
            unit: 'KG',
            outOfStock: true,
            lowStock: true,
            unitCost: 25,
          },
          {
            id: 10,
            name: 'Harina',
            active: true,
            stockQuantity: 2,
            unit: 'KG',
            outOfStock: false,
            lowStock: false,
            unitCost: null,
          },
        ],
      }),
    ),
  };
  const reservationApi = {
    findDay: vi.fn(() =>
      of([
        {
          id: 11,
          status: 'BOOKED',
          guestName: 'Laura',
          servicePointName: 'Mesa 5',
          guestCount: 3,
          startAt: '2026-10-15T22:00:00Z',
        },
        {
          id: 12,
          status: 'SEATED',
          guestName: 'Mario',
          servicePointName: 'Mesa 2',
          guestCount: 2,
          startAt: '2026-10-15T20:00:00Z',
        },
      ]),
    ),
  };
  const promotionApi = { findAll: vi.fn(() => of([{ id: 3, name: 'Comida' }])) };
  const reportApi = { findDaily: vi.fn(() => of({ tips: { netAmount: 125 } })) };
  const realtime = { watchBranch: vi.fn(() => events.asObservable()) };
  const auth = {
    user: () => ({ fullName: 'Ana' }),
    canOperateCash: () => false,
    canViewReports: () => false,
    canAdminister: () => false,
    isOwner: () => false,
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    auth.canAdminister = () => false;
    auth.canOperateCash = () => false;
    auth.canViewReports = () => false;
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: OrderApiService, useValue: ordersApi },
        { provide: PreparationApiService, useValue: preparationApi },
        { provide: CashRegisterApiService, useValue: cashApi },
        { provide: InventoryReportApiService, useValue: inventoryApi },
        { provide: ReservationApiService, useValue: reservationApi },
        { provide: PromotionApiService, useValue: promotionApi },
        { provide: DailyReportApiService, useValue: reportApi },
        { provide: RealtimeService, useValue: realtime },
        { provide: AuthApiService, useValue: auth },
      ],
    }).compileComponents();
  });

  it('shows live restaurant counts and the Pro edition without exposing restricted data', () => {
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Essential Pro');
    expect(text).toContain('Restaurante principal');
    expect(text).toContain('Pedidos activos');
    expect(text).toContain('Mesas ocupadas');
    expect(text).toContain('1 por iniciar · 1 en proceso');
    expect(text).toContain('Mesa 7');
    expect(text).toContain('Carlos');
    expect(text).not.toContain('TURNO ACTUAL');
    expect(text).not.toContain('PRO · INVENTARIO');
    expect(text).not.toContain('PRO · RECEPCIÓN');
    expect(cashApi.findCurrent).not.toHaveBeenCalled();
    expect(inventoryApi.report).not.toHaveBeenCalled();
    expect(reservationApi.findDay).not.toHaveBeenCalled();
    expect(reportApi.findDaily).not.toHaveBeenCalled();
    expect(realtime.watchBranch).toHaveBeenCalledWith(1);

    fixture.destroy();
  });

  it('shows real stock alerts and warns when the valuation is partial for administrators', () => {
    auth.canAdminister = () => true;
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(inventoryApi.report).toHaveBeenCalledOnce();
    expect(text).toContain('PRO · INVENTARIO');
    expect(text).toContain('Tomate');
    expect(text).toContain('Agotado');
    expect(text).toContain('Parcial: 1 insumo(s) sin valorar');
    expect(text).toContain('Recetas');
    expect(text).toContain('Conteo');

    fixture.destroy();
  });

  it('keeps the restaurant operation visible if the inventory report fails', () => {
    auth.canAdminister = () => true;
    inventoryApi.report.mockImplementationOnce(() => throwError(() => new Error('offline')));
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('No se pudo actualizar el inventario.');
    expect(text).toContain('Mesa 7');
    expect(text).toContain('Pedidos activos');

    fixture.destroy();
  });

  it('shows bookings and clients to cashiers without tips or promotion management', () => {
    auth.canOperateCash = () => true;
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(reservationApi.findDay).toHaveBeenCalledWith(
      expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
    );
    expect(text).toContain('PRO · RECEPCIÓN');
    expect(text).toContain('Laura');
    expect(text).toContain('Mesa 5');
    expect(text).toContain('Clientes');
    expect(text).not.toContain('Propinas recibidas hoy');
    expect(promotionApi.findAll).not.toHaveBeenCalled();
    expect(reportApi.findDaily).not.toHaveBeenCalled();

    fixture.destroy();
  });

  it('shows current promotions and daily tips to managers', () => {
    auth.canOperateCash = () => true;
    auth.canViewReports = () => true;
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(promotionApi.findAll).toHaveBeenCalledWith(true);
    expect(reportApi.findDaily).toHaveBeenCalledWith(1, expect.any(String));
    expect(text).toContain('1 disponibles hoy');
    expect(text).toContain('Propinas recibidas hoy');
    expect(text).toContain('125');
    expect(inventoryApi.report).not.toHaveBeenCalled();

    fixture.destroy();
  });

  it('keeps orders visible when the agenda cannot be reached', () => {
    auth.canOperateCash = () => true;
    reservationApi.findDay.mockImplementationOnce(() => throwError(() => new Error('offline')));
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('No se pudo actualizar la agenda.');
    expect(text).toContain('Mesa 7');

    fixture.destroy();
  });
});
