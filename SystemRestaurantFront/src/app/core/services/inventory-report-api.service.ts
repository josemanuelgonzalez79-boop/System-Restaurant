import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  InventoryCount,
  InventoryCountRequest,
  InventoryReport,
} from '../models/inventory-report.model';

@Injectable({ providedIn: 'root' })
export class InventoryReportApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/inventory`;

  report(from: string, to: string): Observable<InventoryReport> {
    return this.http.get<InventoryReport>(`${this.url}/report`, {
      params: new HttpParams().set('from', from).set('to', to),
    });
  }

  counts(): Observable<InventoryCount[]> {
    return this.http.get<InventoryCount[]>(`${this.url}/counts`);
  }

  reconcile(payload: InventoryCountRequest): Observable<InventoryCount> {
    return this.http.post<InventoryCount>(`${this.url}/counts`, payload);
  }
}
