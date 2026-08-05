import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { EssentialUser, UserCreatePayload, UserUpdatePayload } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/v1/users`;

  findAll(): Observable<EssentialUser[]> {
    return this.http.get<EssentialUser[]>(this.url);
  }

  create(payload: UserCreatePayload): Observable<EssentialUser> {
    return this.http.post<EssentialUser>(this.url, payload);
  }

  update(id: number, payload: UserUpdatePayload): Observable<EssentialUser> {
    return this.http.put<EssentialUser>(`${this.url}/${id}`, payload);
  }

  changeActive(id: number, active: boolean): Observable<EssentialUser> {
    return this.http.patch<EssentialUser>(`${this.url}/${id}/active`, { active });
  }

  resetPassword(id: number, password: string): Observable<void> {
    return this.http.post<void>(`${this.url}/${id}/password`, { password });
  }
}
