package pl.panzerhund.tracker.listing;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.util.ArrayList;
import java.util.List;

/** Builds a JPA Specification from optional {@link ListingFilter} fields. */
final class ListingSpecifications {

    private ListingSpecifications() {
    }

    static Specification<Listing> matching(ListingFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.category() != null) {
                predicates.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.source() != null) {
                predicates.add(cb.equal(root.get("source"), f.source()));
            }
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.region() != null && !f.region().isBlank()) {
                predicates.add(cb.equal(root.get("region"), f.region()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + f.q().toLowerCase() + "%"));
            }
            if (f.priceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.priceMin()));
            }
            if (f.priceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.priceMax()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
