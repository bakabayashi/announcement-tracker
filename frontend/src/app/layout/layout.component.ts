import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { PopoverModule } from 'primeng/popover';
import { TagModule } from 'primeng/tag';
import { AuthService } from '../core/auth/auth.service';
import { AppNotification, NotificationType } from '../core/models';
import { NotificationStore } from '../core/notifications/notification.store';

@Component({
  selector: 'app-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    DatePipe,
    ButtonModule,
    BadgeModule,
    PopoverModule,
    AvatarModule,
    MenuModule,
    TagModule,
  ],
  template: `
    <header class="topbar">
      <div class="bar">
        <a class="brand" routerLink="/listings"><i class="pi pi-map-marker"></i> Announcement Tracker</a>
        <nav>
          <a routerLink="/listings" routerLinkActive="active">Ogłoszenia</a>
          <a routerLink="/saved" routerLinkActive="active">Zapisane</a>
          <a routerLink="/searches" routerLinkActive="active">Wyszukiwania</a>
          @if (auth.isAdmin()) {
            <a routerLink="/duplicates" routerLinkActive="active">Duplikaty</a>
          }
        </nav>
        <span class="spacer"></span>

        <button type="button" class="bell" (click)="notifPanel.toggle($event)" aria-label="Powiadomienia">
          <i class="pi pi-bell"></i>
          @if (store.unreadCount() > 0) {
            <p-badge [value]="store.unreadCount().toString()" severity="danger" />
          }
        </button>

        <p-avatar
          [image]="auth.user()?.pictureUrl ?? undefined"
          icon="pi pi-user"
          shape="circle"
          class="avatar"
          (click)="userMenu.toggle($event)"
        />
        <p-menu #userMenu [model]="userMenuItems" [popup]="true" />
      </div>
    </header>

    <p-popover #notifPanel>
      <div class="notif">
        <div class="notif-head">
          <strong>Powiadomienia</strong>
          <p-button label="Oznacz wszystkie" [text]="true" size="small" (onClick)="store.markAllRead()" />
        </div>
        @if (store.notifications().length === 0) {
          <p class="muted">Brak powiadomień.</p>
        }
        @for (n of store.notifications(); track n.id) {
          <div class="notif-item" [class.unread]="!n.read" (click)="open(n)">
            <p-tag [value]="typeLabel(n.type)" [severity]="typeSeverity(n.type)" />
            <div class="notif-body">
              <div class="notif-title">{{ n.listing.title }}</div>
              <div class="muted">{{ n.createdAt | date: 'short' }}</div>
            </div>
          </div>
        }
      </div>
    </p-popover>

    <main class="page">
      <router-outlet />
    </main>
  `,
  styles: [
    `
      .topbar {
        background: var(--p-content-background, #fff);
        border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
        position: sticky;
        top: 0;
        z-index: 10;
      }
      .bar {
        max-width: var(--app-max-width);
        margin: 0 auto;
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 0.5rem 1rem;
      }
      .brand {
        font-weight: 600;
        font-size: 1.05rem;
      }
      nav {
        display: flex;
        gap: 1rem;
      }
      nav a {
        padding: 0.35rem 0.25rem;
        color: var(--p-text-muted-color, #6b7280);
        border-bottom: 2px solid transparent;
      }
      nav a.active {
        color: var(--p-primary-color, #3b82f6);
        border-bottom-color: var(--p-primary-color, #3b82f6);
      }
      .bell {
        position: relative;
        background: none;
        border: none;
        cursor: pointer;
        font-size: 1.25rem;
        color: var(--p-text-color, #374151);
      }
      .bell p-badge {
        position: absolute;
        top: -6px;
        right: -10px;
      }
      .avatar {
        cursor: pointer;
      }
      .notif {
        width: 22rem;
        max-height: 24rem;
        overflow: auto;
      }
      .notif-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 0.5rem;
      }
      .notif-item {
        display: flex;
        gap: 0.5rem;
        padding: 0.5rem;
        border-radius: 6px;
        cursor: pointer;
      }
      .notif-item:hover {
        background: var(--p-content-hover-background, #f3f4f6);
      }
      .notif-item.unread {
        background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 8%, transparent);
      }
      .notif-title {
        font-weight: 500;
      }
    `,
  ],
})
export class LayoutComponent implements OnInit, OnDestroy {
  protected auth = inject(AuthService);
  protected store = inject(NotificationStore);
  private router = inject(Router);

  protected userMenuItems: MenuItem[] = [
    { label: 'Wyloguj', icon: 'pi pi-sign-out', command: () => this.logout() },
  ];

  ngOnInit(): void {
    this.store.init();
  }

  ngOnDestroy(): void {
    this.store.disconnect();
  }

  protected open(notification: AppNotification): void {
    this.store.markRead(notification.id);
    this.router.navigate(['/listings', notification.listing.id]);
  }

  private logout(): void {
    this.auth.logout().subscribe({
      next: () => this.afterLogout(),
      error: () => this.afterLogout(),
    });
  }

  private afterLogout(): void {
    this.store.disconnect();
    this.router.navigateByUrl('/login');
  }

  protected typeLabel(type: NotificationType): string {
    switch (type) {
      case 'PRICE_DROP':
        return 'Spadek ceny';
      case 'NEW_MATCH':
        return 'Nowe';
      case 'REPOSTED':
        return 'Wznowione';
    }
  }

  protected typeSeverity(type: NotificationType): 'success' | 'info' | 'warn' {
    switch (type) {
      case 'PRICE_DROP':
        return 'success';
      case 'NEW_MATCH':
        return 'info';
      case 'REPOSTED':
        return 'warn';
    }
  }
}
