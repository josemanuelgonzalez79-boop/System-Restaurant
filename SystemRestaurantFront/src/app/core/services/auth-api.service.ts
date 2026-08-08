import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import { EssentialUser, InitialSetupPayload } from '../models/user.model';
import { environment } from '../../../environments/environment';

interface SetupStatus {
  setupRequired: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/auth`;
  private readonly currentUser = signal<EssentialUser | null>(null);
  private readonly initialized = signal(false);
  private initializationRequest: Observable<boolean> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly setupRequired = signal(false);
  readonly authenticated = computed(() => this.currentUser() !== null);
  readonly isOwner = computed(() => this.currentUser()?.role === 'OWNER');
  readonly canAdminister = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'OWNER' || role === 'ADMIN';
  });

  ensureInitialized(): Observable<boolean> {
    if (this.initialized()) {
      return of(true);
    }
    if (this.initializationRequest) {
      return this.initializationRequest;
    }

    this.initializationRequest = this.refreshCsrf().pipe(
      switchMap(() => this.http.get<SetupStatus>(`${this.url}/setup-status`)),
      tap((status) => this.setupRequired.set(status.setupRequired)),
      switchMap((status) => {
        if (status.setupRequired) {
          return of(null);
        }
        return this.loadCurrentUser();
      }),
      tap((user) => this.currentUser.set(user)),
      map(() => true),
      catchError((error: unknown) => {
        this.currentUser.set(null);
        return of(true);
      }),
      tap(() => this.initialized.set(true)),
      finalize(() => (this.initializationRequest = null)),
      shareReplay(1),
    );

    return this.initializationRequest;
  }

  setup(payload: InitialSetupPayload): Observable<EssentialUser> {
    return this.refreshCsrf().pipe(
      switchMap(() => this.http.post<EssentialUser>(`${this.url}/setup`, payload)),
      tap(() => this.setupRequired.set(false)),
      switchMap(() => this.login(payload.username, payload.password)),
    );
  }

  login(username: string, password: string): Observable<EssentialUser> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.refreshCsrf().pipe(
      switchMap(() =>
        this.http.post<void>(`${this.url}/login`, body.toString(), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      ),
      switchMap(() => this.refreshCsrf()),
      switchMap(() => this.http.get<EssentialUser>(`${this.url}/me`)),
      tap((user) => {
        this.currentUser.set(user);
        this.setupRequired.set(false);
        this.initialized.set(true);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.url}/logout`, {}).pipe(
      switchMap(() => this.refreshCsrf()),
      tap(() => this.currentUser.set(null)),
      map(() => undefined),
    );
  }

  private loadCurrentUser(): Observable<EssentialUser | null> {
    return this.http
      .get<EssentialUser>(`${this.url}/me`)
      .pipe(
        catchError((error: { status?: number }) =>
          error.status === 401 ? of(null) : throwError(() => error),
        ),
      );
  }

  private refreshCsrf(): Observable<unknown> {
    return this.http.get(`${this.url}/csrf`);
  }
}
