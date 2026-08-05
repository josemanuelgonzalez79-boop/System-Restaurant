import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EssentialUser } from '../models/user.model';
import { AuthApiService } from './auth-api.service';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('should detect that the initial owner is required', () => {
    service.ensureInitialized().subscribe((initialized) => {
      expect(initialized).toBe(true);
      expect(service.setupRequired()).toBe(true);
      expect(service.user()).toBeNull();
    });

    httpController.expectOne('/api/v1/auth/csrf').flush({
      token: 'csrf-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    });
    httpController.expectOne('/api/v1/auth/setup-status').flush({ setupRequired: true });
  });

  it('should log in and load the current user', () => {
    const user: EssentialUser = {
      id: 1,
      username: 'admin',
      fullName: 'Propietario',
      role: 'OWNER',
      active: true,
      mustChangePassword: false,
      version: 0,
      createdAt: '2026-07-29T00:00:00Z',
      updatedAt: '2026-07-29T00:00:00Z',
    };

    service.login('admin', 'una-contraseña-segura').subscribe((result) => {
      expect(result).toEqual(user);
      expect(service.user()).toEqual(user);
    });

    httpController.expectOne('/api/v1/auth/csrf').flush({
      token: 'csrf-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    });

    const login = httpController.expectOne('/api/v1/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toContain('username=admin');
    login.flush(null);

    httpController.expectOne('/api/v1/auth/csrf').flush({
      token: 'new-csrf-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    });
    httpController.expectOne('/api/v1/auth/me').flush(user);
  });
});
