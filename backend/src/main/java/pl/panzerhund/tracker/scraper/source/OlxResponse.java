package pl.panzerhund.tracker.scraper.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Subset of the OLX Partner API {@code GET /adverts} response that we consume.
 * Field shapes are based on the public Partner API; some are TODO-flagged for verification
 * against the authenticated docs once real credentials are available.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OlxResponse(List<Advert> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Advert(
            long id,
            String url,
            String title,
            String description,
            List<Param> params,
            Location location,
            MapPoint map
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Param(String key, String name, Value value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(String label, Object value, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(Named city, Named region, Named district) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Named(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MapPoint(Double lat, @JsonProperty("lon") Double lng) {
    }
}
