import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { RealtimeEvent } from '../../core/models/realtime.model';
import { AuthApiService } from '../../core/services/auth-api.service';
import { CashRegisterApiService } from '../../core/services/cash-register-api.service';
import { InventoryReportApiService } from '../../core/services/inventory-report-api.service';
import { OrderApiService } from '../../core/services/order-api.service';
import { PreparationApiService } from '../../core/services/preparation-api.service';
import { RealtimeService } from '../../core/services/realtime.service';
import { Home } from './home';

describe('Inicio Plus', () => {
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
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: OrderApiService, useValue: ordersApi },
        { provide: PreparationApiService, useValue: preparationApi },
        { provide: CashRegisterApiService, useValue: cashApi },
        { provide: InventoryReportApiService, useValue: inventoryApi },
        { provide: RealtimeService, useValue: realtime },
        { provide: AuthApiService, useValue: auth },
      ],
    }).compileComponents();
  });

  it('shows live restaurant counts and the Plus edition without exposing inventory to operators', () => {
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Essential Plus');
    expect(text).toContain('Restaurante principal');
    expect(text).toContain('Pedidos activos');
    expect(text).toContain('Mesas ocupadas');
    expect(text).toContain('1 por iniciar · 1 en proceso');
    expect(text).toContain('Mesa 7');
    expect(text).toContain('Carlos');
    expect(text).not.toContain('TURNO ACTUAL');
    expect(text).not.toContain('PLUS · INVENTARIO');
    expect(cashApi.findCurrent).not.toHaveBeenCalled();
    expect(inventoryApi.report).not.toHaveBeenCalled();
    expect(realtime.watchBranch).toHaveBeenCalledWith(1);

    fixture.destroy();
  });

  it('shows real stock alerts and warns when the valuation is partial for administrators', () => {
    auth.canAdminister = () => true;
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(inventoryApi.report).toHaveBeenCalledOnce();
    expect(text).toContain('PLUS · INVENTARIO');
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
});
