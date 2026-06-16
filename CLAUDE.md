# announcement-tracker

Aplikacja do śledzenia ogłoszeń nieruchomości i samochodów.
Multi-user, self-hosted na Docker.

## Stack
- Backend: Java 21, Spring Boot 3.x, Maven
- Frontend: Angular 20, standalone components, PrimeNG
- Baza: PostgreSQL 16 (JSONB dla atrybutów per kategoria)
- Migracje: Flyway
- Hosting: Docker Compose

## Uruchomienie
```bash
docker-compose up -d                 # pełne środowisko
cd backend && mvn spring-boot:run    # backend dev
cd frontend && npm start             # frontend dev
```

## Struktura backendu
Pakiet bazowy: `pl.panzerhund.tracker`

Moduły (osobny pakiet każdy):
- `scraper`         — pobieranie ogłoszeń z serwisów
- `listing`         — model ogłoszenia, historia cen
- `search`          — kryteria wyszukiwania i filtry
- `notification`    — powiadomienia (bell w UI)
- `deduplication`   — wykrywanie i scalanie duplikatów
- `category`        — typy: PLOT (działki), CAR (samochody)
- `user`            — encja użytkownika, OAuth2
- `security`        — konfiguracja Spring Security

Każdy moduł: controller / service / repository / dto / entity / mapper

## Auth
Spring Security OAuth2 Login — Google jako jedyny provider.

Encja `User`: id (UUID), google_sub, email, name, picture_url, created_at.
Wszystkie encje per-user (SavedListing, SearchCriteria, Notification) mają `user_id` FK.

Whitelist:
- Properta: `app.security.allowed-emails` (lista stringów)
- Gdy lista pusta → wszyscy zalogowani Google przechodzą (dev mode)
- Gdy niepusta → blokujesz każdego spoza listy (403)
- Logika w `OAuth2UserService` dekoratorze w `SecurityConfig`

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile

app:
  security:
    allowed-emails: []  # puste = dev mode, wszyscy przechodzą
```

## Źródła danych
- OLX — REST API (https://developer.olx.pl)
- Otodom — HTTP client + JSON parsing
- Otomoto — HTTP client + JSON parsing
- Allegro Lokalnie — HTTP client + JSON parsing

Każde źródło implementuje interfejs `ListingSource`.
Osobny `*Scraper` bean per serwis.

## Strategia scrapingu

### Criteria-driven
Scraper NIE pobiera wszystkiego. Dla każdego `SearchCriteria` z bazy:
- buduje query z filtrów JSONB (kategoria, region, cena min/max, powierzchnia itd.)
- wykonuje request do serwisu z tymi parametrami
- przetwarza wyniki

### Incremental
Wyniki sortowane od najnowszych. Paginacja przerywa się gdy:
- trafimy na `external_id` który już istnieje w bazie (last_seen_at < 24h temu)
- osiągniemy `app.scraper.max-pages-per-criteria` (domyślnie: 5)

### Rate limiting
- Delay między requestami do tego samego serwisu: losowe 3–8 sekund
- `ScraperRateLimiter` bean, jeden per serwis

### Scheduler
NIE używać `@Scheduled(cron)`. Programatyczny `ThreadPoolTaskScheduler`:
- Okno: 22:00–04:00 (360 minut)
- Po każdym zakończeniu → losuj następny start w oknie
- `ScraperSchedulingService` oblicza `nextRun`
- Poza oknem → zaplanuj na najbliższe 22:xx

```java
// pseudokod
int offsetMinutes = ThreadLocalRandom.current().nextInt(0, 360);
LocalDateTime nextRun = nextWindowStart().plusMinutes(offsetMinutes);
```

### TTL i cleanup
`CleanupScheduler` odpala się codziennie o 05:00:
- Listing niewidziany > 30 dni → status `INACTIVE`
- Listing `INACTIVE` starszy niż 90 dni → DELETE (pomija SavedListing)
- PriceHistory starszy niż 365 dni → DELETE (pomija SavedListing)

### Konfiguracja
```yaml
app:
  scraper:
    max-pages-per-criteria: 5
    delay-min-seconds: 3
    delay-max-seconds: 8
    window-start: "22:00"
    window-duration-minutes: 360
```

## Model danych

**User**
- id (UUID), google_sub, email, name, picture_url, created_at

**Listing**
- source (OLX/OTODOM/OTOMOTO/ALLEGRO), external_id, category (PLOT/CAR)
- title, description, price, currency, url
- location: city, region, lat, lng (nullable)
- attributes: JSONB
- status: ACTIVE / INACTIVE / MERGED
- first_seen_at, last_seen_at, user_id

**PriceHistory** — insert-only, nigdy nie edytujemy
- listing_id, price, currency, recorded_at

**SavedListing**
- user_id, listing_id, notes, saved_at

**SearchCriteria**
- user_id, name, category, filters: JSONB

**Notification**
- user_id, listing_id, type (PRICE_DROP/NEW_MATCH/REPOSTED), is_read, created_at

**DuplicateGroup**
- primary_listing_id, status (SUGGESTED/CONFIRMED/REJECTED)

## Deduplication
Dwa poziomy:
1. Pewny match: ten sam source + external_id → update, nie insert
2. Prawdopodobny duplikat (sugestia):
    - ta sama kategoria + lokalizacja ±1km + powierzchnia ±5% + cena ±15%
    - różne source LUB nowy external_id na tym samym source
      → `DuplicateGroup` ze statusem SUGGESTED, user akceptuje/odrzuca w UI

## Powiadomienia
Tylko bell w UI, bez email/push.
- Stan początkowy: `GET /api/v1/notifications/unread-count`
- Live update: SSE `GET /api/v1/notifications/stream` (text/event-stream) — serwer pushuje przy nowym powiadomieniu
- Panel: lista z is_read, oznaczanie jako przeczytane
- Typy: PRICE_DROP, NEW_MATCH, REPOSTED

## Wykresy
PrimeNG `p-chart` (Chart.js) — NIE Grafana.

Wykres historii ceny (standalone `PriceHistoryChartComponent`):
- `GET /api/v1/listings/{id}/price-history` → `PriceHistoryResponse[]`
- `GET /api/v1/listings/{id}/price-stats` → średnia i mediana dla podobnych
  (ta sama kategoria + region, ostatnie 30 dni)
- Dwie linie: historia ceny ogłoszenia + średnia rynkowa

## API
- Base path: `/api/v1/`
- OpenAPI: `http://localhost:8080/swagger-ui.html`

## Konwencje backendu
- Constructor injection / @RequiredArgsConstructor (NIE @Autowired)
- DTOs jako Java records, suffix Request/Response
- Bean Validation na Request DTO
- @ControllerAdvice GlobalExceptionHandler
- Testy: JUnit 5 + Mockito, suffix Test

## Konwencje frontendu
- Standalone components, brak NgModules
- inject() zamiast constructor injection
- PrimeNG jako biblioteka UI, theme: Aura
- Serwisy API: `*ApiService` wrappujące HttpClient

## Docker Compose
Serwisy: `backend`, `frontend` (nginx), `postgres`
Dane postgres na named volume.
Sekrety wyłącznie przez zmienne środowiskowe.

## Zasady pracy agenta
- Przed implementacją czegokolwiek większego: opisz plan, czekaj na akceptację
- Po napisaniu kodu: git add, commit, push
- NIE uruchamiaj mvn verify ani ng build lokalnie
- Sprawdź wynik CI: gh run watch && gh run view --log --exit-status
- Jeśli CI fail: przeczytaj logi, popraw, commit, push ponownie
- Po naprawieniu CI faila: squash commitów (failujące + fix), push --force-with-lease 
- Jeden commit = jedna logiczna zmiana
- Jeśli coś jest niejasne — pytaj, nie zgaduj
- Nigdy nie modyfikuj istniejących migracji Flyway