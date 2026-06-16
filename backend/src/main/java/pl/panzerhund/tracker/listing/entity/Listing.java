package pl.panzerhund.tracker.listing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.panzerhund.tracker.category.entity.Category;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ogłoszenie. GLOBALNE - jedno ogłoszenie = jeden rekord, bez user_id. */
@Entity
@Table(
        name = "listings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_listings_source_external",
                columnNames = {"source", "external_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Source source;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 255)
    private String city;

    @Column(length = 255)
    private String region;

    private Double lat;

    private Double lng;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> attributes = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ListingStatus status;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
}
