package pl.panzerhund.tracker.deduplication.entity;

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
import pl.panzerhund.tracker.listing.entity.Listing;

import java.util.UUID;

/** Członek grupy duplikatów (ogłoszenie należące do grupy). */
@Entity
@Table(
        name = "duplicate_group_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_duplicate_group_members",
                columnNames = {"group_id", "listing_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class DuplicateGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private DuplicateGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
}
