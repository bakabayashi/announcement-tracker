import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { SearchCriteriaApiService } from '../../core/api/search-criteria-api.service';
import { CATEGORIES, Category, SearchCriteria } from '../../core/models';

interface Pair {
  key: string;
  value: string;
}

@Component({
  selector: 'app-search-criteria',
  imports: [FormsModule, TableModule, ButtonModule, DialogModule, InputTextModule, SelectModule],
  template: `
    <div class="toolbar">
      <h2>Wyszukiwania</h2>
      <span class="spacer"></span>
      <p-button label="Nowe wyszukiwanie" icon="pi pi-plus" (onClick)="openCreate()" />
    </div>

    <p-table [value]="items()" [loading]="loading()" dataKey="id">
      <ng-template pTemplate="header">
        <tr>
          <th>Nazwa</th>
          <th>Kategoria</th>
          <th>Filtry</th>
          <th></th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-item>
        <tr>
          <td>{{ item.name }}</td>
          <td>{{ categoryLabel(item.category) }}</td>
          <td class="muted">{{ filtersSummary(item) }}</td>
          <td class="actions">
            <p-button icon="pi pi-pencil" [text]="true" (onClick)="openEdit(item)" />
            <p-button icon="pi pi-trash" severity="danger" [text]="true" (onClick)="remove(item)" />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr><td colspan="4" class="muted">Brak zapisanych wyszukiwań.</td></tr>
      </ng-template>
    </p-table>

    <p-dialog
      [(visible)]="visible"
      [header]="editingId ? 'Edytuj wyszukiwanie' : 'Nowe wyszukiwanie'"
      [modal]="true"
      [style]="{ width: '34rem' }"
    >
      <div class="form">
        <div class="field">
          <label>Nazwa</label>
          <input pInputText [(ngModel)]="name" />
        </div>
        <div class="field">
          <label>Kategoria</label>
          <p-select
            [options]="categoryOptions"
            [(ngModel)]="category"
            optionLabel="label"
            optionValue="value"
          />
        </div>
        <div class="field">
          <label>Filtry</label>
          @for (pair of pairs; track $index) {
            <div class="pair">
              <input pInputText [(ngModel)]="pair.key" placeholder="klucz (np. region)" />
              <input pInputText [(ngModel)]="pair.value" placeholder="wartość" />
              <p-button icon="pi pi-times" [text]="true" (onClick)="removePair($index)" />
            </div>
          }
          <p-button label="Dodaj filtr" icon="pi pi-plus" [text]="true" (onClick)="addPair()" />
        </div>
      </div>
      <ng-template pTemplate="footer">
        <p-button label="Anuluj" [text]="true" (onClick)="visible = false" />
        <p-button label="Zapisz" icon="pi pi-save" [disabled]="!name || !category" (onClick)="save()" />
      </ng-template>
    </p-dialog>
  `,
  styles: [
    `
      .actions {
        white-space: nowrap;
      }
      .form {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }
      .pair {
        display: flex;
        gap: 0.5rem;
        align-items: center;
        margin-bottom: 0.4rem;
      }
      .pair input {
        flex: 1;
      }
    `,
  ],
})
export class SearchCriteriaComponent implements OnInit {
  private api = inject(SearchCriteriaApiService);
  private messages = inject(MessageService);

  protected readonly items = signal<SearchCriteria[]>([]);
  protected readonly loading = signal(false);

  protected visible = false;
  protected editingId: string | null = null;
  protected name = '';
  protected category: Category | null = null;
  protected pairs: Pair[] = [];

  protected categoryOptions = CATEGORIES.map((c) => ({ label: this.categoryLabel(c), value: c }));

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.name = '';
    this.category = null;
    this.pairs = [{ key: '', value: '' }];
    this.visible = true;
  }

  openEdit(item: SearchCriteria): void {
    this.editingId = item.id;
    this.name = item.name;
    this.category = item.category;
    this.pairs = Object.entries(item.filters ?? {}).map(([key, value]) => ({
      key,
      value: String(value),
    }));
    if (this.pairs.length === 0) {
      this.pairs = [{ key: '', value: '' }];
    }
    this.visible = true;
  }

  addPair(): void {
    this.pairs.push({ key: '', value: '' });
  }

  removePair(index: number): void {
    this.pairs.splice(index, 1);
  }

  save(): void {
    if (!this.name || !this.category) {
      return;
    }
    const filters: Record<string, unknown> = {};
    for (const pair of this.pairs) {
      const key = pair.key.trim();
      if (key) {
        filters[key] = pair.value;
      }
    }
    const request = { name: this.name, category: this.category, filters };
    const call = this.editingId
      ? this.api.update(this.editingId, request)
      : this.api.create(request);
    call.subscribe((saved) => {
      this.items.update((list) =>
        this.editingId ? list.map((i) => (i.id === saved.id ? saved : i)) : [...list, saved],
      );
      this.visible = false;
      this.messages.add({ severity: 'success', summary: 'OK', detail: 'Zapisano', life: 2500 });
    });
  }

  remove(item: SearchCriteria): void {
    this.api.remove(item.id).subscribe(() => {
      this.items.update((list) => list.filter((i) => i.id !== item.id));
      this.messages.add({ severity: 'success', summary: 'OK', detail: 'Usunięto', life: 2500 });
    });
  }

  filtersSummary(item: SearchCriteria): string {
    const entries = Object.entries(item.filters ?? {});
    if (entries.length === 0) {
      return '—';
    }
    return entries.map(([key, value]) => `${key}=${value}`).join(', ');
  }

  categoryLabel(category: Category): string {
    return category === 'PLOT' ? 'Działki' : 'Samochody';
  }
}
