import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CashMovementCreatePayload,
  CashRegisterOpenPayload,
  CashRegisterSummary,
} from '../models/cash-register.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CashRegisterApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/cash-registers`;

  findCurrent(branchId: number): Observable<CashRegisterSummary | null> {
    const params = new HttpParams().set('branchId', branchId);
    return this.http.get<CashRegisterSummary | null>(`${this.url}/current`, { params });
  }

  findHistory(branchId: number): Observable<CashRegisterSummary[]> {
    const params = new HttpParams().set('branchId', branchId);
    return this.http.get<CashRegisterSummary[]>(this.url, { params });
  }

  open(payload: CashRegisterOpenPayload): Observable<CashRegisterSummary> {
    return this.http.post<CashRegisterSummary>(`${this.url}/open`, payload);
  }

  addMovement(
    sessionId: number,
    payload: CashMovementCreatePayload,
  ): Observable<CashRegisterSummary> {
    return this.http.post<CashRegisterSummary>(`${this.url}/${sessionId}/movements`, payload);
  }

  voidMovement(
    sessionId: number,
    movementId: number,
    sessionVersion: number,
    reason: string,
  ): Observable<CashRegisterSummary> {
    return this.http.patch<CashRegisterSummary>(
      `${this.url}/${sessionId}/movements/${movementId}/void`,
      { sessionVersion, reason },
    );
  }

  close(
    sessionId: number,
    sessionVersion: number,
    countedCash: number,
    notes: string,
  ): Observable<CashRegisterSummary> {
    return this.http.post<CashRegisterSummary>(`${this.url}/${sessionId}/close`, {
      sessionVersion,
      countedCash,
      notes,
    });
  }
}
