import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ListingApiService } from '../../core/api/listing-api.service';
import { SavedListingApiService } from '../../core/api/saved-listing-api.service';
import { Listing, ListingStatus, SavedListing } from '../../core/models';
import { PriceHistoryChartComponent } from '../../shared/price-history-chart.component';

@Component({
  selector: 'app-listing-detail',
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    RouterLink,
    CardModule,
    ButtonModule,
    TagModule,
    PriceHistoryChartComponent,
  ],
  template: `
    <p-button label="Wróć" icon="pi pi-arrow-left" [text]="true" routerLink="/listings" />

    @if (listing(); as l) {
      <div class="grid">
        <p-card>
          <div class="head">
            <h2>{{ l.title }}</h2>
            <p-tag [value]="l.status" [severity]="statusSeverity(l.status)" />
          </div>
          <p class="meta">
            <span>{{ l.source }}</span> ·
            <span>{{ categoryLabel(l) }}</span> ·
            <span>{{ l.city || '—' }}{{ l.region ? ', ' + l.region : '' }}</span>
          </p>
          <p class="price">
            @if (l.price !== null) {
              {{ l.price | number: '1.0-2' }} {{ l.currency }}
            } @else {
              <span class="muted">brak ceny</span>
            }
          </p>
          @if (l.description) {
            <p>{{ l.description }}</p>
          }
          <a [href]="l.url" target="_blank" rel="noopener">
            <p-button label="Zobacz oryginał" icon="pi pi-external-link" [outlined]="true" />
          </a>

          @if (attributeEntries(l).length > 0) {
            <h3>Atrybuty</h3>
            <table class="attrs">
              @for (attr of attributeEntries(l); track attr[0]) {
                <tr>
                  <td class="muted">{{ attr[0] }}</td>
                  <td>{{ display(attr[1]) }}</td>
                </tr>
              }
            </table>
          }
        </p-card>

        <p-card>
          <h3>Historia ceny</h3>
          <div class="chart">
            <app-price-history-chart [listingId]="l.id" />
          </div>
        </p-card>

        <p-card>
          <h3>Zapisane</h3>
          @if (saved(); as s) {
            <textarea
              class="notes"
              [(ngModel)]="notes"
              rows="3"
              placeholder="Twoje notatki..."
            ></textarea>
            <div class="actions">
              <p-button label="Zapisz notatkę" icon="pi pi-save" (onClick)="updateNotes(s)" />
              <p-button
                label="Usuń z zapisanych"
                icon="pi pi-trash"
                severity="danger"
                [outlined]="true"
                (onClick)="remove(s)"
              />
            </div>
            <p class="muted">Zapisano: {{ s.savedAt | date: 'short' }}</p>
          } @else {
            <p-button label="Zapisz ogłoszenie" icon="pi pi-bookmark" (onClick)="save(l)" />
          }
        </p-card>
      </div>
    }
  `,
  styles: [
    `
      .grid {
        display: grid;
        gap: 1rem;
        grid-template-columns: 1fr;
        margin-top: 1rem;
      }
      @media (min-width: 900px) {
        .grid {
          grid-template-columns: 2fr 1fr;
        }
        .grid > p-card:nth-child(2) {
          grid-column: 1 / 2;
        }
      }
      .head {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        align-items: start;
      }
      .head h2 {
        margin: 0;
      }
      .price {
        font-size: 1.4rem;
        font-weight: 600;
      }
      .chart {
        height: 320px;
      }
      .attrs {
        width: 100%;
        border-collapse: collapse;
      }
      .attrs td {
        padding: 0.25rem 0.5rem;
        border-bottom: 1px solid var(--p-content-border-color, #eee);
      }
      .notes {
        width: 100%;
        padding: 0.5rem;
        border: 1px solid var(--p-content-border-color, #d1d5db);
        border-radius: 6px;
        font: inherit;
      }
      .actions {
        display: flex;
        gap: 0.5rem;
        margin: 0.5rem 0;
        flex-wrap: wrap;
      }
    `,
  ],
})
export class ListingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private api = inject(ListingApiService);
  private savedApi = inject(SavedListingApiService);
  private messages = inject(MessageService);

  protected readonly listing = signal<Listing | null>(null);
  protected readonly saved = signal<SavedListing | null>(null);
  protected notes = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get(id).subscribe((l) => this.listing.set(l));
    this.savedApi.list().subscribe((list) => {
      const entry = list.find((s) => s.listing.id === id) ?? null;
      this.saved.set(entry);
      this.notes = entry?.notes ?? '';
    });
  }

  save(listing: Listing): void {
    this.savedApi.save({ listingId: listing.id, notes: this.notes || null }).subscribe((entry) => {
      this.saved.set(entry);
      this.notes = entry.notes ?? '';
      this.toast('Zapisano ogłoszenie');
    });
  }

  updateNotes(entry: SavedListing): void {
    this.savedApi.updateNotes(entry.id, { notes: this.notes || null }).subscribe((updated) => {
      this.saved.set(updated);
      this.toast('Notatka zapisana');
    });
  }

  remove(entry: SavedListing): void {
    this.savedApi.remove(entry.id).subscribe(() => {
      this.saved.set(null);
      this.notes = '';
      this.toast('Usunięto z zapisanych');
    });
  }

  private toast(detail: string): void {
    this.messages.add({ severity: 'success', summary: 'OK', detail, life: 2500 });
  }

  attributeEntries(listing: Listing): [string, unknown][] {
    return Object.entries(listing.attributes ?? {});
  }

  display(value: unknown): string {
    return value === null || value === undefined ? '—' : String(value);
  }

  categoryLabel(listing: Listing): string {
    return listing.category === 'PLOT' ? 'Działki' : 'Samochody';
  }

  statusSeverity(status: ListingStatus): 'success' | 'secondary' | 'contrast' {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'secondary';
      case 'MERGED':
        return 'contrast';
    }
  }
}
