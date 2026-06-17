import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { SavedListingApiService } from '../../core/api/saved-listing-api.service';
import { SavedListing } from '../../core/models';

@Component({
  selector: 'app-saved-listings',
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink, TableModule, ButtonModule, DialogModule],
  template: `
    <h2>Zapisane ogłoszenia</h2>

    <p-table [value]="items()" [loading]="loading()" dataKey="id">
      <ng-template pTemplate="header">
        <tr>
          <th>Tytuł</th>
          <th>Lokalizacja</th>
          <th>Cena</th>
          <th>Notatka</th>
          <th>Zapisano</th>
          <th></th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-item>
        <tr>
          <td><a [routerLink]="['/listings', item.listing.id]">{{ item.listing.title }}</a></td>
          <td>{{ item.listing.city || '—' }}{{ item.listing.region ? ', ' + item.listing.region : '' }}</td>
          <td>
            @if (item.listing.price !== null) {
              {{ item.listing.price | number: '1.0-2' }} {{ item.listing.currency }}
            } @else {
              <span class="muted">—</span>
            }
          </td>
          <td class="notes-cell">{{ item.notes || '—' }}</td>
          <td>{{ item.savedAt | date: 'short' }}</td>
          <td class="actions">
            <p-button icon="pi pi-pencil" [text]="true" (onClick)="openEdit(item)" />
            <p-button icon="pi pi-trash" severity="danger" [text]="true" (onClick)="remove(item)" />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr><td colspan="6" class="muted">Nie masz jeszcze zapisanych ogłoszeń.</td></tr>
      </ng-template>
    </p-table>

    <p-dialog [(visible)]="editVisible" header="Notatka" [modal]="true" [style]="{ width: '28rem' }">
      <textarea class="notes" [(ngModel)]="editNotes" rows="4"></textarea>
      <ng-template pTemplate="footer">
        <p-button label="Anuluj" [text]="true" (onClick)="editVisible = false" />
        <p-button label="Zapisz" icon="pi pi-save" (onClick)="saveNotes()" />
      </ng-template>
    </p-dialog>
  `,
  styles: [
    `
      .actions {
        white-space: nowrap;
      }
      .notes-cell {
        max-width: 18rem;
      }
      .notes {
        width: 100%;
        padding: 0.5rem;
        border: 1px solid var(--p-content-border-color, #d1d5db);
        border-radius: 6px;
        font: inherit;
      }
    `,
  ],
})
export class SavedListingsComponent implements OnInit {
  private api = inject(SavedListingApiService);
  private messages = inject(MessageService);

  protected readonly items = signal<SavedListing[]>([]);
  protected readonly loading = signal(false);

  protected editVisible = false;
  protected editNotes = '';
  private editing: SavedListing | null = null;

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

  openEdit(item: SavedListing): void {
    this.editing = item;
    this.editNotes = item.notes ?? '';
    this.editVisible = true;
  }

  saveNotes(): void {
    if (!this.editing) {
      return;
    }
    this.api.updateNotes(this.editing.id, { notes: this.editNotes || null }).subscribe((updated) => {
      this.items.update((list) => list.map((i) => (i.id === updated.id ? updated : i)));
      this.editVisible = false;
      this.messages.add({ severity: 'success', summary: 'OK', detail: 'Notatka zapisana', life: 2500 });
    });
  }

  remove(item: SavedListing): void {
    this.api.remove(item.id).subscribe(() => {
      this.items.update((list) => list.filter((i) => i.id !== item.id));
      this.messages.add({ severity: 'success', summary: 'OK', detail: 'Usunięto', life: 2500 });
    });
  }
}
