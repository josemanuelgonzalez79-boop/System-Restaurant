import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  PreparationStatus,
  PreparationTicket,
} from '../models/preparation.model';

@Injectable({ providedIn: 'root' })
export class PreparationApiService {
  private readonly http = inject(HttpClient);
  private readonly preparationUrl = `${environment.apiBaseUrl}/v1/preparation`;

  findTickets(
    branchId: number,
    destination?: 'PRODUCTION' | 'SERVICE',
    activeOnly = true,
  ): Observable<PreparationTicket[]> {
    let params = new HttpParams()
      .set('branchId', branchId.toString())
      .set('activeOnly', activeOnly.toString());
    if (destination) {
      params = params.set('destination', destination);
    }
    return this.http.get<PreparationTicket[]>(this.preparationUrl, { params });
  }

  changeItemStatus(
    itemId: number,
    status: PreparationStatus,
    version: number,
  ): Observable<PreparationTicket> {
    return this.http.patch<PreparationTicket>(
      `${this.preparationUrl}/items/${itemId}/status`,
      { status, version },
    );
  }
}
