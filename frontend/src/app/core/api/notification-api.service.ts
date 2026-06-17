import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AppNotification, UnreadCount } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private http = inject(HttpClient);
  private readonly base = '/api/v1/notifications';

  list(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(this.base);
  }

  unreadCount(): Observable<UnreadCount> {
    return this.http.get<UnreadCount>(`${this.base}/unread-count`);
  }

  markRead(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${this.base}/read-all`, {});
  }

  /** SSE endpoint URL; consumed via EventSource in the notification store. */
  get streamUrl(): string {
    return `${this.base}/stream`;
  }
}
