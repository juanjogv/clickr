package io.clickr.domain;

import com.google.common.base.MoreObjects;
import io.vertx.mutiny.sqlclient.Row;
import java.time.OffsetDateTime;
import java.util.Objects;

/** URL entity representing a shortened URL mapping. */
public class Url {

  private Long id;
  private String shortCode;
  private String originalUrl;
  private Long clicks;
  private OffsetDateTime createdAt;
  private OffsetDateTime lastClickedAt;

  public Url() {}

  public Url(
      Long id,
      String shortCode,
      String originalUrl,
      Long clicks,
      OffsetDateTime createdAt,
      OffsetDateTime lastClickedAt) {
    this.id = id;
    this.shortCode = shortCode;
    this.originalUrl = originalUrl;
    this.clicks = clicks;
    this.createdAt = createdAt;
    this.lastClickedAt = lastClickedAt;
  }

  public static Url from(Row row) {
    return new Url(
        row.getLong("id"),
        row.getString("short_code"),
        row.getString("original_url"),
        row.getLong("clicks"),
        row.getOffsetDateTime("created_at"),
        row.getOffsetDateTime("last_clicked_at"));
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public void setOriginalUrl(String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public Long getClicks() {
    return clicks;
  }

  public void setClicks(Long clicks) {
    this.clicks = clicks;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getLastClickedAt() {
    return lastClickedAt;
  }

  public void setLastClickedAt(OffsetDateTime lastClickedAt) {
    this.lastClickedAt = lastClickedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Url url = (Url) o;
    return Objects.equals(id, url.id) && Objects.equals(shortCode, url.shortCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, shortCode);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("shortCode", shortCode)
        .add("originalUrl", originalUrl)
        .add("clicks", clicks)
        .add("createdAt", createdAt)
        .add("lastClickedAt", lastClickedAt)
        .toString();
  }
}
