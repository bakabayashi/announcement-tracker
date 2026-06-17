package pl.panzerhund.tracker.scraper.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * Helpers shared by the Next.js sources (Otodom, Otomoto): extracting the {@code __NEXT_DATA__} JSON
 * blob from an HTML page and null-safe navigation/reading of nested fields.
 */
final class NextData {

    private NextData() {}

    /** Extracts the JSON body of the {@code __NEXT_DATA__} script tag, or null if absent. */
    static String extract(String html) {
        if (html == null) {
            return null;
        }
        int marker = html.indexOf("__NEXT_DATA__");
        if (marker < 0) {
            return null;
        }
        int open = html.indexOf('>', marker);
        int close = open >= 0 ? html.indexOf("</script>", open) : -1;
        if (open < 0 || close < 0) {
            return null;
        }
        return html.substring(open + 1, close).trim();
    }

    static String str(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }

    static String text(JsonNode node, String... path) {
        JsonNode current = walk(node, path);
        return current.isValueNode() && !current.isNull() ? current.asText() : null;
    }

    static Double dbl(JsonNode node, String... path) {
        JsonNode current = walk(node, path);
        return current.isNumber() ? current.asDouble() : null;
    }

    static BigDecimal dec(JsonNode node, String... path) {
        JsonNode current = walk(node, path);
        return current.isNumber() ? current.decimalValue() : null;
    }

    private static JsonNode walk(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            current = current.path(segment);
        }
        return current;
    }
}
