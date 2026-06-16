package pl.panzerhund.tracker.deduplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.util.UUID;

/** Duplicate group. primaryListing = canonical listing. */
@Entity
@Table(name = "duplicate_groups")
@Getter
@Setter
@NoArgsConstructor
public class DuplicateGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_listing_id", nullable = false)
    private Listing primaryListing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DuplicateStatus status;
}
