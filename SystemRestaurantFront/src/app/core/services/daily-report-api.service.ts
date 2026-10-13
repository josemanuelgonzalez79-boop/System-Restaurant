import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { DailyReport } from '../models/daily-report.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DailyReportApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/reports/daily`;

  findDaily(branchId: number, date: string): Observable<DailyReport> {
    const params = new HttpParams().set('branchId', branchId).set('date', date);
    return this.http.get<DailyReport>(this.url, { params });
  }
}
