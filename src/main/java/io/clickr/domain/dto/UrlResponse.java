package io.clickr.domain.dto;

import io.clickr.domain.Url;
import java.time.OffsetDateTime;

public record UrlResponse(
    String shortCode,
    String shortUrl,
    String originalUrl,
    Long clicks,
    OffsetDateTime createdAt,
    OffsetDateTime lastClickedAt) {
  public static UrlResponse from(Url url, String baseUrl) {
    return new UrlResponse(
        url.getShortCode(),
        baseUrl + "/" + url.getShortCode(),
        url.getOriginalUrl(),
        url.getClicks(),
        url.getCreatedAt(),
        url.getLastClickedAt());
  }
}
