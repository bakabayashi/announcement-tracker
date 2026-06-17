import { Injectable, NgZone, computed, inject, signal } from '@angular/core';
import { NotificationApiService } from '../api/notification-api.service';
import { AppNotification } from '../models';

/**
 * Holds the current user's notifications and unread count, kept live via a Server-Sent Events
 * stream. Connected once when the authenticated layout loads.
 */
@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private api = inject(NotificationApiService);
  private zone = inject(NgZone);

  readonly notifications = signal<AppNotification[]>([]);
  readonly unreadCount = computed(() => this.notifications().filter((n) => !n.read).length);

  private eventSource?: EventSource;

  /** Load the initial list and open the live stream (idempotent). */
  init(): void {
    if (this.eventSource) {
      return;
    }
    this.api.list().subscribe((list) => this.notifications.set(list));
    this.connect();
  }

  private connect(): void {
    const source = new EventSource(this.api.streamUrl);
    source.addEventListener('notification', (event) => {
      const incoming = JSON.parse((event as MessageEvent).data) as AppNotification;
      // EventSource callbacks run outside Angular; re-enter so signals update the view.
      this.zone.run(() => {
        this.notifications.update((list) => [incoming, ...list.filter((n) => n.id !== incoming.id)]);
      });
    });
    this.eventSource = source;
  }

  markRead(id: string): void {
    this.api.markRead(id).subscribe(() => {
      this.notifications.update((list) =>
        list.map((n) => (n.id === id ? { ...n, read: true } : n)),
      );
    });
  }

  markAllRead(): void {
    this.api.markAllRead().subscribe(() => {
      this.notifications.update((list) => list.map((n) => ({ ...n, read: true })));
    });
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = undefined;
  }
}
