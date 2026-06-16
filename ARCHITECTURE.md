# Architektura — announcement-tracker

Dokument projektowy wyprowadzony z `CLAUDE.md`. Opisuje docelową strukturę aplikacji
do śledzenia ogłoszeń nieruchomości (działki) i samochodów. Multi-user, self-hosted (Docker).

Stack: Java 21 / Spring Boot 3.x / Maven · Angular 20 (standalone, PrimeNG) · PostgreSQL 16 (JSONB) · Flyway.

> Sekcje oznaczone **(założenie)** wykraczają poza CLAUDE.md i wymagają potwierdzenia — patrz
> [Założenia poza CLAUDE.md](#założenia-poza-claudemd) na końcu.

---

## 1. Diagram modułów i zależności

Pakiet bazowy: `pl.panzerhund.tracker`. Każdy moduł to osobny pakiet
(controller / service / repository / dto / entity / mapper).

```
                         ┌───────────────┐
                         │    common     │  config, GlobalExceptionHandler,
                         │  (cross-cut)  │  paginacja, błędy — bez zależności
                         └───────▲───────┘
                                 │ (używany przez wszystkie)
        ┌────────────────────────┼────────────────────────┐
        │                        │                         │
   ┌────┴────┐              ┌─────┴─────┐             ┌─────┴─────┐
   │  user   │◄─────────────│ security  │             │ category  │  (typy PLOT/CAR,
   │ (encja, │   dekoruje   │  OAuth2   │             │ słownik   │   schemat atrybutów)
   │  OAuth2)│   UserService│  Login    │             │ atrybutów)│   bez zależności
   └────▲────┘              └───────────┘             └─────▲─────┘
        │ SavedListing / Notification (user_id)             │ kategoria
        │                                                   │
   ┌────┴───────────────────────────────────────────────────┴────┐
   │                          listing                             │
   │   Listing, PriceHistory, SavedListing  (+ wykresy cen)       │
   └───▲──────────────▲─────────────────▲──────────────────▲──────┘
       │              │                 │                  │
  ┌────┴────┐   ┌─────┴───────┐   ┌──────┴───────┐   ┌──────┴──────┐
  │ search  │   │notification │   │deduplication │   │   scraper   │
  │kryteria,│   │ bell w UI   │   │ wykrywanie/  │   │ orkiestracja│
  │ filtry  │   │             │   │ scalanie dup │   │ + źródła    │
  └────▲────┘   └─────────────┘   └──────────────┘   └─────────────┘
       │                                                    │
       └──────────────── scraper czyta SearchCriteria ◄─────┘
```

### Kierunek zależności (kto → kogo używa)

| Moduł           | Zależy od                                              |
|-----------------|--------------------------------------------------------|
| `common`        | — (baza, używany przez wszystkie)                      |
| `category`      | `common`                                               |
| `user`          | `common`                                               |
| `security`      | `user`, `common` (dekoruje `OAuth2UserService`)        |
| `listing`       | `user`, `category`, `common`                           |
| `search`        | `user`, `category`, `common`                           |
| `notification`  | `user`, `listing`, `common`                            |
| `deduplication` | `listing`, `category`, `common`                        |
| `scraper`       | `search`, `listing`, `deduplication`, `notification`, `category` |

Zasada: zależności jednokierunkowe, bez cykli. `scraper` jest na szczycie (orkiestruje),
`common` na dole. Moduły komunikują się przez interfejsy serwisów, nie przez repozytoria innych modułów.

---

## 2. Encje (model danych)

Wszystkie encje per-user mają `user_id` (FK → `users.id`). UUID jako PK.

### User
| Pole          | Typ          | Uwagi                              |
|---------------|--------------|------------------------------------|
| id            | UUID         | PK                                 |
| google_sub    | String       | unikalny, subject z Google OIDC    |
| email         | String       | unikalny                           |
| name          | String       |                                    |
| picture_url   | String       | nullable                           |
| created_at    | Instant      |                                    |

### Listing
| Pole           | Typ            | Uwagi                                        |
|----------------|----------------|----------------------------------------------|
| id             | UUID           | PK                                           |
| source         | enum           | OLX / OTODOM / OTOMOTO / ALLEGRO             |
| external_id    | String         | id w serwisie źródłowym                       |
| category       | enum           | PLOT / CAR                                    |
| title          | String         |                                              |
| description    | String (text)  |                                              |
| price          | BigDecimal     |                                              |
| currency       | String         | np. PLN                                       |
| url            | String         | link do ogłoszenia                           |
| city           | String         | location                                     |
| region         | String         | location                                     |
| lat            | Double         | nullable                                     |
| lng            | Double         | nullable                                     |
| attributes     | JSONB          | atrybuty per kategoria                        |
| status         | enum           | ACTIVE / INACTIVE / MERGED                    |
| first_seen_at  | Instant        |                                              |
| last_seen_at   | Instant        |                                              |

`Listing` jest **globalny** — jedno ogłoszenie = jeden rekord, niezależnie od liczby userów.
Brak `user_id`. Relacja user↔listing idzie wyłącznie przez `SavedListing` i `Notification`.

Klucz naturalny dedup: `(source, external_id)` — unikalny.

### PriceHistory  *(insert-only, nigdy nie edytujemy)*
| Pole         | Typ        | Uwagi                |
|--------------|------------|----------------------|
| id           | UUID       | PK                   |
| listing_id   | UUID       | FK → listings        |
| price        | BigDecimal |                      |
| currency     | String     |                      |
| recorded_at  | Instant    |                      |

### SavedListing
| Pole        | Typ     | Uwagi                |
|-------------|---------|----------------------|
| id          | UUID    | PK                   |
| user_id     | UUID    | FK → users           |
| listing_id  | UUID    | FK → listings        |
| notes       | String  | nullable             |
| saved_at    | Instant |                      |

Unikalność: `(user_id, listing_id)`.

### SearchCriteria
| Pole       | Typ    | Uwagi                |
|------------|--------|----------------------|
| id         | UUID   | PK                   |
| user_id    | UUID   | FK → users           |
| name       | String |                      |
| category   | enum   | PLOT / CAR           |
| filters    | JSONB  | region, cena min/max, powierzchnia itd. |

### Notification
| Pole        | Typ     | Uwagi                                  |
|-------------|---------|----------------------------------------|
| id          | UUID    | PK                                     |
| user_id     | UUID    | FK → users                             |
| listing_id  | UUID    | FK → listings                          |
| type        | enum    | PRICE_DROP / NEW_MATCH / REPOSTED      |
| is_read     | boolean | default false                          |
| created_at  | Instant |                                        |

### DuplicateGroup
| Pole                | Typ   | Uwagi                                   |
|---------------------|-------|-----------------------------------------|
| id                  | UUID  | PK                                      |
| primary_listing_id  | UUID  | FK → listings (ogłoszenie kanoniczne)   |
| status              | enum  | SUGGESTED / CONFIRMED / REJECTED        |

### DuplicateGroupMember  **(założenie)**
Grupa potrzebuje listy członków — w CLAUDE.md nie ma tabeli łączącej.
| Pole              | Typ  | Uwagi                          |
|-------------------|------|--------------------------------|
| id                | UUID | PK                             |
| group_id          | UUID | FK → duplicate_groups          |
| listing_id        | UUID | FK → listings                  |

Unikalność: `(group_id, listing_id)`.

---

## 3. Endpointy REST

Base path: `/api/v1/`. Auth: sesja OAuth2 (cookie). Wszystkie DTO to Java records,
suffix `Request` / `Response`. Listy stronicowane zwracają `Page<T>` (Spring).

### Auth (Spring Security, nie własny controller)
| Metoda | Ścieżka                          | Opis                          |
|--------|----------------------------------|-------------------------------|
| GET    | `/oauth2/authorization/google`   | start logowania Google        |
| GET    | `/login/oauth2/code/google`      | callback OAuth2               |
| POST   | `/logout`                        | wylogowanie                   |

### user
| Metoda | Ścieżka          | DTO in | DTO out        |
|--------|------------------|--------|----------------|
| GET    | `/api/v1/me`     | —      | `UserResponse` |

### category
| Metoda | Ścieżka              | DTO in | DTO out              |
|--------|----------------------|--------|----------------------|
| GET    | `/api/v1/categories` | —      | `CategoryResponse[]` (typ + schemat atrybutów) |

### listing
| Metoda | Ścieżka                                    | DTO in            | DTO out                   |
|--------|--------------------------------------------|-------------------|---------------------------|
| GET    | `/api/v1/listings`                         | query params      | `Page<ListingResponse>`   |
| GET    | `/api/v1/listings/{id}`                     | —                 | `ListingResponse`         |
| GET    | `/api/v1/listings/{id}/price-history`       | —                 | `PriceHistoryResponse[]`  |
| GET    | `/api/v1/listings/{id}/price-stats`         | —                 | `PriceStatsResponse` (średnia + mediana dla podobnych: ta sama kategoria + region, ost. 30 dni) |

### saved-listings (moduł listing)
| Metoda | Ścieżka                          | DTO in                      | DTO out                |
|--------|----------------------------------|-----------------------------|------------------------|
| GET    | `/api/v1/saved-listings`         | —                           | `SavedListingResponse[]` |
| POST   | `/api/v1/saved-listings`         | `SaveListingRequest`        | `SavedListingResponse` |
| PATCH  | `/api/v1/saved-listings/{id}`    | `UpdateSavedListingRequest` (notes) | `SavedListingResponse` |
| DELETE | `/api/v1/saved-listings/{id}`    | —                           | 204                    |

### search
| Metoda | Ścieżka                          | DTO in                  | DTO out                   |
|--------|----------------------------------|-------------------------|---------------------------|
| GET    | `/api/v1/search-criteria`        | —                       | `SearchCriteriaResponse[]` |
| GET    | `/api/v1/search-criteria/{id}`   | —                       | `SearchCriteriaResponse`  |
| POST   | `/api/v1/search-criteria`        | `SearchCriteriaRequest` | `SearchCriteriaResponse`  |
| PUT    | `/api/v1/search-criteria/{id}`   | `SearchCriteriaRequest` | `SearchCriteriaResponse`  |
| DELETE | `/api/v1/search-criteria/{id}`   | —                       | 204                       |

### notification
| Metoda | Ścieżka                                 | DTO in | DTO out                   |
|--------|-----------------------------------------|--------|---------------------------|
| GET    | `/api/v1/notifications`                 | query  | `Page<NotificationResponse>` |
| GET    | `/api/v1/notifications/unread-count`    | —      | `UnreadCountResponse` (stan początkowy) |
| GET    | `/api/v1/notifications/stream`          | —      | SSE `text/event-stream` (live push przy nowym powiadomieniu) |
| POST   | `/api/v1/notifications/{id}/read`       | —      | `NotificationResponse`    |
| POST   | `/api/v1/notifications/read-all`        | —      | 204                       |

### deduplication
| Metoda | Ścieżka                                   | DTO in | DTO out                  |
|--------|-------------------------------------------|--------|--------------------------|
| GET    | `/api/v1/duplicate-groups`                | `?status=SUGGESTED` | `DuplicateGroupResponse[]` |
| POST   | `/api/v1/duplicate-groups/{id}/confirm`   | —      | `DuplicateGroupResponse` |
| POST   | `/api/v1/duplicate-groups/{id}/reject`    | —      | `DuplicateGroupResponse` |

### admin / scraper
| Metoda | Ścieżka                       | DTO in | DTO out             |
|--------|-------------------------------|--------|---------------------|
| POST   | `/api/v1/admin/scraper/run`   | —      | `ScraperRunResponse` |

Manualny trigger scrapera — dostępny dla każdego zalogowanego usera (przydatny do developmentu).
Uruchamia `ScraperOrchestrator` asynchronicznie i zwraca status startu (np. `{ started: true }`).
Poza tym `scraper` działa w tle (scheduler, okno 22:00–04:00).

---

## 4. Struktura pakietów backendu

```
pl.panzerhund.tracker
├── TrackerApplication.java
├── common
│   ├── config            # ObjectMapper, JSONB, CORS, ogólne beany
│   ├── exception         # GlobalExceptionHandler (@ControllerAdvice), ApiError
│   └── dto               # PageResponse / wspólne typy
├── category
│   ├── CategoryController · CategoryService · CategoryRepository
│   ├── dto · entity (Category enum + AttributeSchema) · mapper
├── user
│   ├── UserController · UserService · UserRepository
│   ├── dto (UserResponse) · entity (User) · mapper
├── security
│   ├── SecurityConfig                 # OAuth2 Login, filter chain
│   ├── WhitelistOAuth2UserService     # dekorator: app.security.allowed-emails
│   └── CurrentUser                    # resolver zalogowanego usera
├── listing
│   ├── ListingController · SavedListingController
│   ├── ListingService · PriceHistoryService · PriceStatsService · SavedListingService
│   ├── ListingRepository · PriceHistoryRepository · SavedListingRepository
│   ├── dto (ListingResponse, PriceHistoryResponse, PriceStatsResponse,
│   │        SaveListingRequest, UpdateSavedListingRequest, SavedListingResponse)
│   ├── entity (Listing, PriceHistory, SavedListing, ListingStatus, Source)
│   └── mapper
├── search
│   ├── SearchCriteriaController · SearchCriteriaService · SearchCriteriaRepository
│   ├── dto (SearchCriteriaRequest/Response) · entity (SearchCriteria) · mapper
├── notification
│   ├── NotificationController · NotificationService · NotificationRepository
│   ├── dto (NotificationResponse, UnreadCountResponse)
│   ├── entity (Notification, NotificationType) · mapper
├── deduplication
│   ├── DuplicateGroupController · DeduplicationService · DuplicateGroupRepository
│   ├── DuplicateDetector              # reguły: lokalizacja ±1km, pow. ±5%, cena ±15%
│   ├── dto (DuplicateGroupResponse) · entity (DuplicateGroup, DuplicateGroupMember, DuplicateStatus)
│   └── mapper
└── scraper
    ├── ScraperAdminController         # POST /api/v1/admin/scraper/run (trigger manualny)
    ├── ListingSource (interfejs)
    ├── source
    │   ├── OlxScraper · OtodomScraper · OtomotoScraper · AllegroLokalnieScraper
    ├── ScraperOrchestrator            # criteria-driven, incremental
    ├── ScraperRateLimiter             # 3–8 s, jeden bean per serwis
    ├── ScraperSchedulingService       # ThreadPoolTaskScheduler, okno 22:00–04:00
    ├── CleanupScheduler               # codziennie 05:00, TTL/cleanup
    └── config (ScraperProperties @ConfigurationProperties)
```

Konwencje (z CLAUDE.md): constructor injection / `@RequiredArgsConstructor` (nie `@Autowired`),
DTO jako records, Bean Validation na Request, testy JUnit 5 + Mockito (suffix `Test`).

---

## 5. Struktura komponentów frontendu

Angular 20, standalone components (bez NgModules), `inject()`, PrimeNG (theme Aura),
serwisy API `*ApiService` wrappujące `HttpClient`.

```
src/app
├── app.component.ts                  # shell + layout
├── core
│   ├── api
│   │   ├── user-api.service.ts
│   │   ├── category-api.service.ts
│   │   ├── listing-api.service.ts
│   │   ├── saved-listing-api.service.ts
│   │   ├── search-api.service.ts
│   │   ├── notification-api.service.ts
│   │   └── duplicate-api.service.ts
│   ├── interceptors                  # auth/credentials, błędy
│   └── models                        # interfejsy DTO (mirror backendu)
├── layout
│   ├── toolbar.component.ts          # pasek górny
│   └── notification-bell.component.ts# badge unread, SSE (EventSource)
├── features
│   ├── auth
│   │   └── login.component.ts
│   ├── listings
│   │   ├── listings-list.component.ts        # p-table + filtry
│   │   ├── listing-detail.component.ts
│   │   └── price-history-chart.component.ts  # p-chart, 2 linie (cena + średnia rynkowa)
│   ├── saved
│   │   └── saved-listings.component.ts
│   ├── search
│   │   ├── search-criteria-list.component.ts
│   │   └── search-criteria-form.component.ts # dynamiczny formularz wg schematu kategorii
│   ├── notifications
│   │   └── notifications-panel.component.ts   # lista, mark-as-read
│   └── deduplication
│       └── duplicate-groups.component.ts      # accept/reject sugestii
└── app.routes.ts                     # routing standalone
```

`PriceHistoryChartComponent` (standalone) konsumuje `/price-history` + `/price-stats`
i rysuje dwie linie (historia ceny ogłoszenia + średnia rynkowa) przez PrimeNG `p-chart`.

---

## 6. Plan migracji Flyway

Lokalizacja: `backend/src/main/resources/db/migration`. Nazewnictwo `V{n}__opis.sql`.
**Nigdy nie modyfikujemy istniejącej migracji** — zmiany tylko jako nowe `V{n+1}`.

Schemat początkowy to **jedna migracja** (`V1__init_schema.sql`) — wszystkie tabele
powstają naraz, więc dzielenie ich na osobne pliki nie ma sensu. Osobne `V{n}` rezerwujemy
dla późniejszych, przyrostowych zmian (np. nowa kolumna, nowy indeks, poluzowanie ograniczenia).

| Wersja | Plik                  | Co tworzy                                                                 |
|--------|-----------------------|---------------------------------------------------------------------------|
| V1     | `V1__init_schema.sql` | komplet tabel: `users`, `listings` (globalny, bez `user_id`; `attributes JSONB`; enumy varchar+check; unique `(source, external_id)`; `price`/`currency`/`city`/`region` nullable), `price_history`, `saved_listings` (unique `(user_id, listing_id)`), `search_criteria` (`filters JSONB`), `notifications`, `duplicate_groups` + `duplicate_group_members` — oraz indeksy: `listings(status, last_seen_at)`, `listings(category, region)`, GIN na `listings.attributes` i `search_criteria.filters`, `notifications(user_id, is_read)`, `price_history(listing_id, recorded_at)` |

Kolejność `CREATE TABLE` w pliku wymuszona zależnościami FK (users → listings → reszta).
Seed/dane referencyjne kategorii (PLOT/CAR) — w kodzie/enumie, nie w migracji.

---

## Założenia poza CLAUDE.md

Elementy dodane w celu spójności, których CLAUDE.md nie precyzuje — **do akceptacji lub korekty**:

1. **`DuplicateGroupMember`** (tabela w `V1__init_schema.sql`) — grupa duplikatów potrzebuje listy członków;
   CLAUDE.md wymienia tylko `DuplicateGroup` z `primary_listing_id`.
2. **Endpointy `saved-listings`, `search-criteria` CRUD, `categories`, `/me`** — niewymienione
   wprost, ale wynikają z modelu danych i UI.
3. **Indeksy** — w tej samej migracji `V1__init_schema.sql` co tabele (w tym GIN dla JSONB).
4. **`POST /api/v1/admin/scraper/run`** — manualny trigger scrapera dla zalogowanego usera
   (development); poza tym `scraper` działa w tle (scheduler).
5. **`PriceStatsResponse`** jako DTO dla `/price-stats` (średnia + mediana).

---

**Czekam na Twoją akceptację** tego dokumentu (oraz decyzji z sekcji *Założenia*) przed
jakąkolwiek implementacją.
