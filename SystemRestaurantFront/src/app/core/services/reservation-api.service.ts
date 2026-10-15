import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Reservation, ReservationEvent, ReservationSavePayload } from '../models/reservation.model';

@Injectable({ providedIn: 'root' })
export class ReservationApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/reservations`;

  findDay(date: string): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(this.url, { params: new HttpParams().set('date', date) });
  }

  history(id: number): Observable<ReservationEvent[]> {
    return this.http.get<ReservationEvent[]>(`${this.url}/${id}/history`);
  }

  create(payload: ReservationSavePayload): Observable<Reservation> {
    return this.http.post<Reservation>(this.url, payload);
  }

  update(id: number, payload: ReservationSavePayload): Observable<Reservation> {
    return this.http.put<Reservation>(`${this.url}/${id}`, payload);
  }

  cancel(id: number, version: number, reason: string): Observable<Reservation> {
    return this.http.post<Reservation>(`${this.url}/${id}/cancel`, {
      expectedVersion: version,
      reason,
    });
  }

  noShow(id: number, version: number): Observable<Reservation> {
    return this.http.post<Reservation>(`${this.url}/${id}/no-show`, {
      expectedVersion: version,
    });
  }
}
