import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ListingApiService } from '../../core/api/listing-api.service';
import {
  CATEGORIES,
  Category,
  LISTING_STATUSES,
  Listing,
  ListingStatus,
  SOURCES,
  Source,
} from '../../core/models';

interface Option<T> {
  label: string;
  value: T;
}

@Component({
  selector: 'app-listings-list',
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    TableModule,
    TagModule,
    ButtonModule,
    SelectModule,
    InputTextModule,
    InputNumberModule,
  ],
  template: `
    <h2>Ogłoszenia</h2>

    <div class="toolbar">
      <div class="field">
        <label>Kategoria</label>
        <p-select
          [options]="categoryOptions"
          [(ngModel)]="category"
          optionLabel="label"
          optionValue="value"
          [showClear]="true"
          placeholder="Wszystkie"
        />
      </div>
      <div class="field">
        <label>Źródło</label>
        <p-select
          [options]="sourceOptions"
          [(ngModel)]="source"
          optionLabel="label"
          optionValue="value"
          [showClear]="true"
          placeholder="Wszystkie"
        />
      </div>
      <div class="field">
        <label>Status</label>
        <p-select
          [options]="statusOptions"
          [(ngModel)]="status"
          optionLabel="label"
          optionValue="value"
          [showClear]="true"
          placeholder="Wszystkie"
        />
      </div>
      <div class="field">
        <label>Region</label>
        <input pInputText [(ngModel)]="region" placeholder="np. mazowieckie" />
      </div>
      <div class="field">
        <label>Szukaj</label>
        <input pInputText [(ngModel)]="q" placeholder="tytuł / opis" />
      </div>
      <div class="field">
        <label>Cena min</label>
        <p-inputNumber [(ngModel)]="priceMin" [useGrouping]="false" />
      </div>
      <div class="field">
        <label>Cena max</label>
        <p-inputNumber [(ngModel)]="priceMax" [useGrouping]="false" />
      </div>
      <p-button label="Filtruj" icon="pi pi-filter" (onClick)="applyFilters()" />
      <p-button label="Wyczyść" icon="pi pi-times" [outlined]="true" (onClick)="clear()" />
    </div>

    <p-table
      [value]="rows()"
      [lazy]="true"
      (onLazyLoad)="load($event)"
      [paginator]="true"
      [rows]="size"
      [totalRecords]="total()"
      [loading]="loading()"
      [rowsPerPageOptions]="[10, 20, 50]"
      dataKey="id"
    >
      <ng-template pTemplate="header">
        <tr>
          <th pSortableColumn="title">Tytuł</th>
          <th>Źródło</th>
          <th>Kategoria</th>
          <th pSortableColumn="price">Cena</th>
          <th>Lokalizacja</th>
          <th>Status</th>
          <th pSortableColumn="lastSeenAt">Ostatnio</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-row>
        <tr class="row" (click)="goTo(row)">
          <td>{{ row.title }}</td>
          <td>{{ row.source }}</td>
          <td>{{ categoryLabel(row.category) }}</td>
          <td>
            @if (row.price !== null) {
              {{ row.price | number: '1.0-2' }} {{ row.currency }}
            } @else {
              <span class="muted">—</span>
            }
          </td>
          <td>{{ row.city || '—' }}{{ row.region ? ', ' + row.region : '' }}</td>
          <td><p-tag [value]="row.status" [severity]="statusSeverity(row.status)" /></td>
          <td>{{ row.lastSeenAt | date: 'short' }}</td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="7" class="muted">Brak ogłoszeń dla wybranych filtrów.</td>
        </tr>
      </ng-template>
    </p-table>
  `,
  styles: [
    `
      .row {
        cursor: pointer;
      }
    `,
  ],
})
export class ListingsListComponent implements OnInit {
  private api = inject(ListingApiService);
  private router = inject(Router);

  protected readonly rows = signal<Listing[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected size = 20;
  private first = 0;
  private sort: string | null = null;

  protected category: Category | null = null;
  protected source: Source | null = null;
  protected status: ListingStatus | null = null;
  protected region: string | null = null;
  protected q: string | null = null;
  protected priceMin: number | null = null;
  protected priceMax: number | null = null;

  protected categoryOptions: Option<Category>[] = CATEGORIES.map((c) => ({
    label: this.categoryLabel(c),
    value: c,
  }));
  protected sourceOptions: Option<Source>[] = SOURCES.map((s) => ({ label: s, value: s }));
  protected statusOptions: Option<ListingStatus>[] = LISTING_STATUSES.map((s) => ({
    label: s,
    value: s,
  }));

  ngOnInit(): void {
    // p-table fires onLazyLoad on init; no explicit first fetch needed.
  }

  load(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.size = event.rows ?? this.size;
    const sortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
    this.sort = sortField ? `${sortField},${event.sortOrder === -1 ? 'desc' : 'asc'}` : null;
    this.fetch();
  }

  applyFilters(): void {
    this.first = 0;
    this.fetch();
  }

  clear(): void {
    this.category = null;
    this.source = null;
    this.status = null;
    this.region = null;
    this.q = null;
    this.priceMin = null;
    this.priceMax = null;
    this.applyFilters();
  }

  private fetch(): void {
    this.loading.set(true);
    this.api
      .list({
        category: this.category,
        source: this.source,
        status: this.status,
        region: this.region,
        q: this.q,
        priceMin: this.priceMin,
        priceMax: this.priceMax,
        page: Math.floor(this.first / this.size),
        size: this.size,
        sort: this.sort,
      })
      .subscribe({
        next: (pageData) => {
          this.rows.set(pageData.content);
          this.total.set(pageData.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  goTo(row: Listing): void {
    this.router.navigate(['/listings', row.id]);
  }

  categoryLabel(category: Category): string {
    return category === 'PLOT' ? 'Działki' : 'Samochody';
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
