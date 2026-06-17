import { Component, OnInit, inject, input, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ChartModule } from 'primeng/chart';
import { ListingApiService } from '../core/api/listing-api.service';

@Component({
  selector: 'app-price-history-chart',
  imports: [ChartModule],
  template: `
    @if (hasData()) {
      <p-chart type="line" [data]="data()" [options]="options" />
    } @else {
      <p class="muted">Brak historii cen dla tego ogłoszenia.</p>
    }
  `,
})
export class PriceHistoryChartComponent implements OnInit {
  private api = inject(ListingApiService);

  readonly listingId = input.required<string>();

  protected readonly data = signal<unknown>(undefined);
  protected readonly hasData = signal(false);

  protected readonly options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
    scales: { y: { beginAtZero: false } },
  };

  ngOnInit(): void {
    const id = this.listingId();
    forkJoin({ history: this.api.priceHistory(id), stats: this.api.priceStats(id) }).subscribe(
      ({ history, stats }) => {
        if (history.length === 0) {
          this.hasData.set(false);
          return;
        }
        const labels = history.map((point) => new Date(point.recordedAt).toLocaleDateString('pl-PL'));
        const datasets: unknown[] = [
          {
            label: 'Cena ogłoszenia',
            data: history.map((point) => point.price),
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59,130,246,0.15)',
            tension: 0.3,
            fill: false,
          },
        ];
        if (stats.average !== null) {
          datasets.push({
            label: 'Średnia rynkowa',
            data: labels.map(() => stats.average),
            borderColor: '#f59e0b',
            borderDash: [6, 6],
            pointRadius: 0,
            fill: false,
          });
        }
        this.data.set({ labels, datasets });
        this.hasData.set(true);
      },
    );
  }
}
