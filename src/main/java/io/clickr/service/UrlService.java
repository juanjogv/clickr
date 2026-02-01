package io.clickr.service;

import io.clickr.domain.dto.CreateUrlRequest;
import io.clickr.domain.dto.UrlResponse;
import io.clickr.repository.UrlRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service layer for URL shortening business logic. */
@ApplicationScoped
public class UrlService {

  private static final Logger log = LoggerFactory.getLogger(UrlService.class);

  private final UrlRepository urlRepository;
  private final ShortCodeGenerator shortCodeGenerator;

  @ConfigProperty(name = "clickr.base-url", defaultValue = "http://localhost:8080")
  String baseUrl;

  @Inject
  public UrlService(UrlRepository urlRepository, ShortCodeGenerator shortCodeGenerator) {
    this.urlRepository = urlRepository;
    this.shortCodeGenerator = shortCodeGenerator;
  }

  /**
   * Creates a new short URL.
   *
   * @param request the create URL request
   * @return Uni with the URL response
   */
  public Uni<UrlResponse> createShortUrl(CreateUrlRequest request) {
    log.info("Creating short URL for: {}", request.url());

    // Validate URL (additional validation beyond bean validation)
    if (!isValidUrl(request.url())) {
      return Uni.createFrom()
          .failure(new IllegalArgumentException("Invalid URL format or potentially malicious URL"));
    }

    // Get next sequence value and generate short code
    return urlRepository
        .getNextSequenceValue()
        .flatMap(
            id -> {
              String shortCode = shortCodeGenerator.encode(id);
              log.info("Generated short code: {} for ID: {}", shortCode, id);

              // Create the URL entity
              return urlRepository
                  .create(request.url(), shortCode)
                  .map(url -> UrlResponse.from(url, baseUrl))
                  .invoke(
                      response ->
                          log.info("Successfully created short URL: {}", response.shortUrl()));
            })
        .onFailure()
        .invoke(
            throwable -> {
              log.error("Failed to create short URL for: {}", request.url());
              log.error(throwable.getMessage(), throwable);
            });
  }

  /**
   * Retrieves URL information by short code.
   *
   * @param shortCode the short code
   * @return Uni with Optional of URL response
   */
  public Uni<Optional<UrlResponse>> getUrlByShortCode(String shortCode) {
    log.info("Retrieving URL info for short code: {}", shortCode);

    // Validate short code format
    if (!shortCodeGenerator.isValid(shortCode)) {
      log.warn("Invalid short code format: {}", shortCode);
      return Uni.createFrom().item(Optional.empty());
    }

    return urlRepository
        .findByShortCode(shortCode)
        .map(optionalUrl -> optionalUrl.map(url -> UrlResponse.from(url, baseUrl)))
        .invoke(
            response -> {
              if (response.isEmpty()) {
                log.warn("Short code not found: {}", shortCode);
              } else {
                log.info(
                    "Found URL for short code {}: {}", shortCode, response.get().originalUrl());
              }
            });
  }

  /**
   * Deletes a URL by its short code.
   *
   * @param shortCode the short code
   * @return Uni with boolean indicating success
   */
  public Uni<Boolean> deleteUrl(String shortCode) {
    log.info("Deleting URL with short code: {}", shortCode);

    return urlRepository
        .deleteByShortCode(shortCode)
        .map(
            count -> {
              boolean deleted = count > 0;
              if (deleted) {
                log.info("Successfully deleted short code: {}", shortCode);
              } else {
                log.warn("Short code not found for deletion: {}", shortCode);
              }
              return deleted;
            });
  }

  /**
   * Increments the click counter for a URL by its short code.
   *
   * @param shortCode the short code of the URL
   * @return Uni that completes when the increment operation finishes
   */
  public Uni<Void> incrementClicks(String shortCode) {
    log.info("Incrementing clicks for short code: {}", shortCode);
    return urlRepository.incrementClickByShortCode(shortCode);
  }

  /**
   * Finds the original URL by its short code
   *
   * @param shortCode the short code to search for
   * @return Uni with Optional containing the original URL, or empty if not found
   */
  public Uni<Optional<String>> findOriginalUrlByShortCode(String shortCode) {
    log.info("Finding original URL for short code: {}", shortCode);
    return urlRepository.findOriginalUrlByShortCode(shortCode);
  }

  /**
   * Additional URL validation to prevent SSRF and malicious URLs.
   *
   * @param url the URL to validate
   * @return true if valid, false otherwise
   */
  private boolean isValidUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }

    // Prevent javascript: and data: schemes
    String lowerUrl = url.toLowerCase();
    if (lowerUrl.startsWith("javascript:")
        || lowerUrl.startsWith("data:")
        || lowerUrl.startsWith("file:")) {
      return false;
    }

    // Must be http or https
    if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
      return false;
    }

    // Prevent localhost and private IPs (basic SSRF protection)
    if (lowerUrl.contains("localhost")
        || lowerUrl.contains("127.0.0.1")
        || lowerUrl.matches(".*https?://(10|172\\.(1[6-9]|2[0-9]|3[01])|192\\.168)\\..*")) {
      return false;
    }

    return true;
  }
}
