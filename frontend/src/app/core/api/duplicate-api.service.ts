import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DuplicateGroup } from '../models';

@Injectable({ providedIn: 'root' })
export class DuplicateApiService {
  private http = inject(HttpClient);
  private readonly base = '/api/v1/duplicates';

  list(): Observable<DuplicateGroup[]> {
    return this.http.get<DuplicateGroup[]>(this.base);
  }

  confirm(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/confirm`, {});
  }

  reject(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/reject`, {});
  }
}
