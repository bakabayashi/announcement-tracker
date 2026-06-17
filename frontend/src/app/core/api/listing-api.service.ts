import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Listing, ListingQuery, PageResponse, PriceHistoryPoint, PriceStats } from '../models';

@Injectable({ providedIn: 'root' })
export class ListingApiService {
  private http = inject(HttpClient);
  private readonly base = '/api/v1/listings';

  list(query: ListingQuery): Observable<PageResponse<Listing>> {
    let params = new HttpParams();
    const entries: [string, unknown][] = [
      ['category', query.category],
      ['source', query.source],
      ['status', query.status],
      ['region', query.region],
      ['q', query.q],
      ['priceMin', query.priceMin],
      ['priceMax', query.priceMax],
      ['page', query.page],
      ['size', query.size],
      ['sort', query.sort],
    ];
    for (const [key, value] of entries) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<Listing>>(this.base, { params });
  }

  get(id: string): Observable<Listing> {
    return this.http.get<Listing>(`${this.base}/${id}`);
  }

  priceHistory(id: string): Observable<PriceHistoryPoint[]> {
    return this.http.get<PriceHistoryPoint[]>(`${this.base}/${id}/price-history`);
  }

  priceStats(id: string): Observable<PriceStats> {
    return this.http.get<PriceStats>(`${this.base}/${id}/price-stats`);
  }
}
