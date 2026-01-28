package io.clickr.service;

import jakarta.enterprise.context.ApplicationScoped;

/** Generates short codes using Base62 encoding. Base62 = [0-9A-Za-z] (62 characters) */
@ApplicationScoped
public class ShortCodeGenerator {

  private static final String BASE62_CHARS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final int BASE = BASE62_CHARS.length();

  /**
   * Encodes a numeric ID to a Base62 string.
   *
   * <p>Examples: - 1 -> "1" - 62 -> "10" - 1000 -> "g8" - 1000000 -> "4c92"
   *
   * @param id the numeric ID to encode
   * @return the Base62 encoded string
   */
  public String encode(long id) {
    if (id == 0) {
      return String.valueOf(BASE62_CHARS.charAt(0));
    }

    StringBuilder encoded = new StringBuilder();
    while (id > 0) {
      int remainder = (int) (id % BASE);
      encoded.append(BASE62_CHARS.charAt(remainder));
      id = id / BASE;
    }

    return encoded.reverse().toString();
  }

  /**
   * Decodes a Base62 string back to a numeric ID.
   *
   * @param shortCode the Base62 string to decode
   * @return the numeric ID
   * @throws IllegalArgumentException if the short code contains invalid characters
   */
  public long decode(String shortCode) {
    if (shortCode == null || shortCode.isEmpty()) {
      throw new IllegalArgumentException("Short code cannot be null or empty");
    }

    long decoded = 0;
    for (int i = 0; i < shortCode.length(); i++) {
      char c = shortCode.charAt(i);
      int index = BASE62_CHARS.indexOf(c);
      if (index == -1) {
        throw new IllegalArgumentException("Invalid character in short code: " + c);
      }
      decoded = decoded * BASE + index;
    }

    return decoded;
  }

  /**
   * Validates if a string is a valid Base62 short code.
   *
   * @param shortCode the string to validate
   * @return true if valid, false otherwise
   */
  public boolean isValid(String shortCode) {
    if (shortCode == null || shortCode.isEmpty()) {
      return false;
    }

    for (char c : shortCode.toCharArray()) {
      if (BASE62_CHARS.indexOf(c) == -1) {
        return false;
      }
    }

    return true;
  }
}
