// TypeScript mirror of the backend DTOs (see /api/v1). Enums are string unions.

export type Category = 'PLOT' | 'CAR';
export type Source = 'OLX' | 'OTODOM' | 'OTOMOTO' | 'ALLEGRO';
export type ListingStatus = 'ACTIVE' | 'INACTIVE' | 'MERGED';
export type NotificationType = 'PRICE_DROP' | 'NEW_MATCH' | 'REPOSTED';
export type DuplicateStatus = 'SUGGESTED' | 'CONFIRMED' | 'REJECTED';
export type Role = 'USER' | 'ADMIN';

export const CATEGORIES: Category[] = ['PLOT', 'CAR'];
export const SOURCES: Source[] = ['OLX', 'OTODOM', 'OTOMOTO', 'ALLEGRO'];
export const LISTING_STATUSES: ListingStatus[] = ['ACTIVE', 'INACTIVE', 'MERGED'];

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Listing {
  id: string;
  source: Source;
  externalId: string;
  category: Category;
  title: string;
  description: string | null;
  price: number | null;
  currency: string | null;
  url: string;
  city: string | null;
  region: string | null;
  lat: number | null;
  lng: number | null;
  attributes: Record<string, unknown>;
  status: ListingStatus;
  firstSeenAt: string;
  lastSeenAt: string;
}

export interface PriceHistoryPoint {
  price: number;
  currency: string;
  recordedAt: string;
}

export interface PriceStats {
  average: number | null;
  median: number | null;
  sampleSize: number;
}

export interface SavedListing {
  id: string;
  listing: Listing;
  notes: string | null;
  savedAt: string;
}

export interface SaveListingRequest {
  listingId: string;
  notes?: string | null;
}

export interface UpdateSavedListingRequest {
  notes?: string | null;
}

export interface AppNotification {
  id: string;
  listing: Listing;
  type: NotificationType;
  read: boolean;
  createdAt: string;
}

export interface UnreadCount {
  count: number;
}

export interface SearchCriteria {
  id: string;
  name: string;
  category: Category;
  filters: Record<string, unknown>;
}

export interface SearchCriteriaRequest {
  name: string;
  category: Category;
  filters: Record<string, unknown>;
}

export interface DuplicateListing {
  listingId: string;
  source: Source;
  title: string;
  price: number | null;
  currency: string | null;
  city: string | null;
  region: string | null;
  url: string;
  status: ListingStatus;
}

export interface DuplicateGroup {
  groupId: string;
  status: DuplicateStatus;
  primary: DuplicateListing;
  members: DuplicateListing[];
}

export interface User {
  id: string;
  email: string;
  name: string;
  pictureUrl: string | null;
  role: Role;
}

/** Query params for the listings list endpoint. */
export interface ListingQuery {
  category?: Category | null;
  source?: Source | null;
  status?: ListingStatus | null;
  region?: string | null;
  q?: string | null;
  priceMin?: number | null;
  priceMax?: number | null;
  page?: number;
  size?: number;
  sort?: string | null;
}
