import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SaveListingRequest, SavedListing, UpdateSavedListingRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class SavedListingApiService {
  private http = inject(HttpClient);
  private readonly base = '/api/v1/saved-listings';

  list(): Observable<SavedListing[]> {
    return this.http.get<SavedListing[]>(this.base);
  }

  save(request: SaveListingRequest): Observable<SavedListing> {
    return this.http.post<SavedListing>(this.base, request);
  }

  updateNotes(id: string, request: UpdateSavedListingRequest): Observable<SavedListing> {
    return this.http.patch<SavedListing>(`${this.base}/${id}`, request);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
