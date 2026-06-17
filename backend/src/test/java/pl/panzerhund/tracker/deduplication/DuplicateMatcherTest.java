package pl.panzerhund.tracker.deduplication;

import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateMatcherTest {

    private Listing listing(Category category, String price, String currency, String area) {
        Listing l = new Listing();
        l.setCategory(category);
        l.setPrice(price == null ? null : new BigDecimal(price));
        l.setCurrency(currency);
        Map<String, Object> attributes = new HashMap<>();
        if (area != null) {
            attributes.put("area", area);
        }
        l.setAttributes(attributes);
        return l;
    }

    private Listing plot(String price, String area) {
        return listing(Category.PLOT, price, "PLN", area);
    }

    @Test
    void matchesPlotsWithSimilarPriceAndArea() {
        assertThat(DuplicateMatcher.isProbableDuplicate(plot("100000", "1000"), plot("110000", "1040"))).isTrue();
    }

    @Test
    void rejectsDifferentCategory() {
        Listing plot = listing(Category.PLOT, "100000", "PLN", "1000");
        Listing car = listing(Category.CAR, "100000", "PLN", null);
        assertThat(DuplicateMatcher.isProbableDuplicate(plot, car)).isFalse();
    }

    @Test
    void rejectsWhenPriceDiffersByMoreThanFifteenPercent() {
        assertThat(DuplicateMatcher.isProbableDuplicate(plot("100000", "1000"), plot("120000", "1000"))).isFalse();
    }

    @Test
    void rejectsWhenPlotAreaDiffersByMoreThanFivePercent() {
        assertThat(DuplicateMatcher.isProbableDuplicate(plot("100000", "1000"), plot("100000", "1100"))).isFalse();
    }

    @Test
    void carsIgnoreAreaCriterion() {
        Listing a = listing(Category.CAR, "50000", "PLN", null);
        Listing b = listing(Category.CAR, "52000", "PLN", null);
        assertThat(DuplicateMatcher.isProbableDuplicate(a, b)).isTrue();
    }

    @Test
    void rejectsWhenCurrenciesDiffer() {
        Listing a = listing(Category.PLOT, "100000", "PLN", "1000");
        Listing b = listing(Category.PLOT, "100000", "EUR", "1000");
        assertThat(DuplicateMatcher.isProbableDuplicate(a, b)).isFalse();
    }

    @Test
    void rejectsPlotWhenAreaAttributeMissing() {
        assertThat(DuplicateMatcher.isProbableDuplicate(plot("100000", null), plot("100000", "1000"))).isFalse();
    }
}
