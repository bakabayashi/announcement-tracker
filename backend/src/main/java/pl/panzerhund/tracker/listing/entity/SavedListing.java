package pl.panzerhund.tracker.listing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import pl.panzerhund.tracker.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/** Zapisane ogłoszenie (per-user). */
@Entity
@Table(
        name = "saved_listings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_saved_listings_user_listing",
                columnNames = {"user_id", "listing_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class SavedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;
}
