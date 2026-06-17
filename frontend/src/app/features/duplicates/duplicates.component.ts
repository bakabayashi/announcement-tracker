import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { DuplicateApiService } from '../../core/api/duplicate-api.service';
import { DuplicateGroup, DuplicateListing } from '../../core/models';

@Component({
  selector: 'app-duplicates',
  imports: [DecimalPipe, NgTemplateOutlet, CardModule, ButtonModule, TagModule],
  template: `
    <h2>Sugerowane duplikaty</h2>
    <p class="muted">Scalanie jest globalne i nieodwracalne — dotyczy wszystkich użytkowników.</p>

    @if (groups().length === 0 && !loading()) {
      <p class="muted">Brak sugestii duplikatów.</p>
    }

    @for (group of groups(); track group.groupId) {
      <p-card class="group">
        <div class="cols">
          <div class="col">
            <h4>Główne (zostaje)</h4>
            <ng-container *ngTemplateOutlet="listingTpl; context: { $implicit: group.primary, primary: true }" />
          </div>
          <div class="col">
            <h4>Duplikaty ({{ group.members.length }})</h4>
            @for (member of group.members; track member.listingId) {
              <ng-container *ngTemplateOutlet="listingTpl; context: { $implicit: member, primary: false }" />
            }
          </div>
        </div>
        <div class="actions">
          <p-button label="Scal" icon="pi pi-check" (onClick)="confirm(group)" />
          <p-button label="Odrzuć" icon="pi pi-times" severity="secondary" [outlined]="true" (onClick)="reject(group)" />
        </div>
      </p-card>
    }

    <ng-template #listingTpl let-item let-primary="primary">
      <div class="listing" [class.is-primary]="primary">
        <div class="listing-head">
          <strong>{{ item.title }}</strong>
          <p-tag [value]="item.status" [severity]="statusSeverity(item)" />
        </div>
        <div class="muted">
          {{ item.source }} ·
          {{ item.city || '—' }}{{ item.region ? ', ' + item.region : '' }}
        </div>
        <div class="price">
          @if (item.price !== null) {
            {{ item.price | number: '1.0-2' }} {{ item.currency }}
          } @else {
            <span class="muted">brak ceny</span>
          }
        </div>
        <a [href]="item.url" target="_blank" rel="noopener" class="muted">Zobacz oryginał ↗</a>
      </div>
    </ng-template>
  `,
  styles: [
    `
      .group {
        margin-bottom: 1rem;
      }
      .cols {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
      }
      @media (max-width: 760px) {
        .cols {
          grid-template-columns: 1fr;
        }
      }
      .col h4 {
        margin: 0 0 0.5rem;
      }
      .listing {
        border: 1px solid var(--p-content-border-color, #e5e7eb);
        border-radius: 8px;
        padding: 0.6rem 0.75rem;
        margin-bottom: 0.5rem;
      }
      .listing.is-primary {
        border-color: var(--p-primary-color, #3b82f6);
        background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 6%, transparent);
      }
      .listing-head {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
      }
      .price {
        font-weight: 600;
      }
      .actions {
        display: flex;
        gap: 0.5rem;
        margin-top: 0.75rem;
      }
    `,
  ],
})
export class DuplicatesComponent implements OnInit {
  private api = inject(DuplicateApiService);
  private messages = inject(MessageService);

  protected readonly groups = signal<DuplicateGroup[]>([]);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (list) => {
        this.groups.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  confirm(group: DuplicateGroup): void {
    this.api.confirm(group.groupId).subscribe(() => {
      this.drop(group);
      this.messages.add({ severity: 'success', summary: 'Scalono', detail: 'Duplikaty scalone', life: 2500 });
    });
  }

  reject(group: DuplicateGroup): void {
    this.api.reject(group.groupId).subscribe(() => {
      this.drop(group);
      this.messages.add({ severity: 'info', summary: 'Odrzucono', detail: 'Sugestia odrzucona', life: 2500 });
    });
  }

  private drop(group: DuplicateGroup): void {
    this.groups.update((list) => list.filter((g) => g.groupId !== group.groupId));
  }

  statusSeverity(item: DuplicateListing): 'success' | 'secondary' | 'contrast' {
    switch (item.status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'secondary';
      case 'MERGED':
        return 'contrast';
    }
  }
}
