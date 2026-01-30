package io.clickr.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class RedirectService {

  private static final Logger log = LoggerFactory.getLogger(RedirectService.class);

  private final UrlService urlService;
  private final ShortCodeGenerator shortCodeGenerator;
  private final ManagedExecutor managedExecutor;

  @Inject
  public RedirectService(
      UrlService urlService,
      ShortCodeGenerator shortCodeGenerator,
      ManagedExecutor managedExecutor) {
    this.urlService = urlService;
    this.shortCodeGenerator = shortCodeGenerator;
    this.managedExecutor = managedExecutor;
  }

  /**
   * Handles redirect by finding the URL and incrementing click counter.
   *
   * @param shortCode the short code to redirect
   * @return Uni with Optional of original URL
   */
  public Uni<RestResponse<Void>> redirect(String shortCode) {
    log.trace("Redirecting short code: {}", shortCode);

    // Validate short code format
    if (!shortCodeGenerator.isValid(shortCode)) {
      log.info("Invalid short code format for redirect: {}", shortCode);
      throw new IllegalArgumentException("Invalid short code format for redirect");
    }

    return urlService
        .findOriginalUrlByShortCode(shortCode)
        .map(Optional::orElseThrow)
        .invoke(
            _ ->
                urlService
                    .incrementClicks(shortCode)
                    .runSubscriptionOn(managedExecutor)
                    .subscribe()
                    .with(
                        _ -> log.trace("Incremented clicks for {}", shortCode),
                        failure -> log.error("Failed to increment clicks", failure)
                    )
        )
        .map(originalUrl -> RestResponse.seeOther(URI.create(originalUrl)));
  }
}
