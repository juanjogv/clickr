package io.clickr.domain.dto;

import io.github.aglibs.validcheck.ValidCheck;
import java.util.regex.Pattern;

/** Request DTO for creating a new short URL. */
public record CreateUrlRequest(String url) {

  private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*");

  public CreateUrlRequest {
    ValidCheck.require()
        .notBlank(url, "URL cannot be blank")
        .max(url.length(), 2048, "URL cannot exceed 2048 characters")
        .matches(url, URL_PATTERN, "URL must start with http:// or https://");
  }
}
