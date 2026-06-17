import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SearchCriteria, SearchCriteriaRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class SearchCriteriaApiService {
  private http = inject(HttpClient);
  private readonly base = '/api/v1/search-criteria';

  list(): Observable<SearchCriteria[]> {
    return this.http.get<SearchCriteria[]>(this.base);
  }

  create(request: SearchCriteriaRequest): Observable<SearchCriteria> {
    return this.http.post<SearchCriteria>(this.base, request);
  }

  update(id: string, request: SearchCriteriaRequest): Observable<SearchCriteria> {
    return this.http.put<SearchCriteria>(`${this.base}/${id}`, request);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
