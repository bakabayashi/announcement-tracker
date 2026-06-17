package pl.panzerhund.tracker.deduplication;

import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.math.BigDecimal;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether two listings are probable duplicates (a suggestion, not an exact match).
 * Criteria: same category, price within ±15%, and — for PLOT only — area within ±5%. The geographic
 * ±1 km is applied upstream by the PostGIS query, so it is not re-checked here.
 * Stateless; pure function of the two listings.
 */
public final class DuplicateMatcher {

    static final double PRICE_TOLERANCE = 0.15;
    static final double AREA_TOLERANCE = 0.05;
    /** JSONB attribute key holding the plot surface (m²). */
    static final String AREA_ATTRIBUTE_KEY = "area";

    private static final Pattern LEADING_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private DuplicateMatcher() {
    }

    public static boolean isProbableDuplicate(Listing a, Listing b) {
        if (a.getCategory() != b.getCategory()) {
            return false;
        }
        if (!sameCurrency(a, b) || !priceWithinTolerance(a.getPrice(), b.getPrice())) {
            return false;
        }
        // Area only gates real estate; cars match on category + geo (in SQL) + price.
        if (a.getCategory() == Category.PLOT && !areaWithinTolerance(a, b)) {
            return false;
        }
        return true;
    }

    private static boolean sameCurrency(Listing a, Listing b) {
        String ca = a.getCurrency();
        String cb = b.getCurrency();
        return ca == null || cb == null || ca.equalsIgnoreCase(cb);
    }

    private static boolean priceWithinTolerance(BigDecimal pa, BigDecimal pb) {
        if (pa == null || pb == null) {
            return false;
        }
        double a = pa.doubleValue();
        double b = pb.doubleValue();
        double max = Math.max(Math.abs(a), Math.abs(b));
        if (max == 0) {
            return true; // both zero
        }
        return Math.abs(a - b) <= PRICE_TOLERANCE * max;
    }

    private static boolean areaWithinTolerance(Listing a, Listing b) {
        OptionalDouble areaA = parseArea(a);
        OptionalDouble areaB = parseArea(b);
        if (areaA.isEmpty() || areaB.isEmpty()) {
            return false;
        }
        double max = Math.max(areaA.getAsDouble(), areaB.getAsDouble());
        if (max == 0) {
            return true;
        }
        return Math.abs(areaA.getAsDouble() - areaB.getAsDouble()) <= AREA_TOLERANCE * max;
    }

    /** Reads the area attribute (a source-specific label, e.g. "1200 m²") and parses its leading number. */
    private static OptionalDouble parseArea(Listing listing) {
        Object raw = listing.getAttributes().get(AREA_ATTRIBUTE_KEY);
        if (raw == null) {
            return OptionalDouble.empty();
        }
        String normalized = raw.toString().replace(',', '.');
        Matcher matcher = LEADING_NUMBER.matcher(normalized);
        return matcher.find() ? OptionalDouble.of(Double.parseDouble(matcher.group(1))) : OptionalDouble.empty();
    }
}
