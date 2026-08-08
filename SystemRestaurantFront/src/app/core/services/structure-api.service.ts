import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Branch,
  BranchAssignmentUser,
  BranchPayload,
  OperationalArea,
  OperationalAreaPayload,
  ServicePoint,
  ServicePointPayload,
} from '../models/structure.model';

@Injectable({ providedIn: 'root' })
export class StructureApiService {
  private readonly http = inject(HttpClient);
  private readonly branchesUrl = `${environment.apiBaseUrl}/v1/branches`;
  private readonly areasUrl = `${environment.apiBaseUrl}/v1/areas`;
  private readonly pointsUrl = `${environment.apiBaseUrl}/v1/service-points`;

  findBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(this.branchesUrl);
  }

  createBranch(payload: BranchPayload): Observable<Branch> {
    return this.http.post<Branch>(this.branchesUrl, payload);
  }

  updateBranch(id: number, payload: BranchPayload): Observable<Branch> {
    return this.http.put<Branch>(`${this.branchesUrl}/${id}`, payload);
  }

  changeBranchActive(id: number, active: boolean): Observable<Branch> {
    return this.http.patch<Branch>(`${this.branchesUrl}/${id}/active`, { active });
  }

  findAreas(branchId?: number): Observable<OperationalArea[]> {
    const params = branchId ? new HttpParams().set('branchId', branchId.toString()) : undefined;
    return this.http.get<OperationalArea[]>(this.areasUrl, { params });
  }

  createArea(payload: OperationalAreaPayload): Observable<OperationalArea> {
    return this.http.post<OperationalArea>(this.areasUrl, payload);
  }

  updateArea(id: number, payload: OperationalAreaPayload): Observable<OperationalArea> {
    return this.http.put<OperationalArea>(`${this.areasUrl}/${id}`, payload);
  }

  changeAreaActive(id: number, active: boolean): Observable<OperationalArea> {
    return this.http.patch<OperationalArea>(`${this.areasUrl}/${id}/active`, { active });
  }

  findPoints(areaId?: number): Observable<ServicePoint[]> {
    const params = areaId ? new HttpParams().set('areaId', areaId.toString()) : undefined;
    return this.http.get<ServicePoint[]>(this.pointsUrl, { params });
  }

  createPoint(payload: ServicePointPayload): Observable<ServicePoint> {
    return this.http.post<ServicePoint>(this.pointsUrl, payload);
  }

  updatePoint(id: number, payload: ServicePointPayload): Observable<ServicePoint> {
    return this.http.put<ServicePoint>(`${this.pointsUrl}/${id}`, payload);
  }

  changePointActive(id: number, active: boolean): Observable<ServicePoint> {
    return this.http.patch<ServicePoint>(`${this.pointsUrl}/${id}/active`, { active });
  }

  findAssignments(branchId: number): Observable<BranchAssignmentUser[]> {
    return this.http.get<BranchAssignmentUser[]>(`${this.branchesUrl}/${branchId}/assignments`);
  }

  replaceAssignments(branchId: number, userIds: number[]): Observable<BranchAssignmentUser[]> {
    return this.http.put<BranchAssignmentUser[]>(`${this.branchesUrl}/${branchId}/assignments`, {
      userIds,
    });
  }
}
