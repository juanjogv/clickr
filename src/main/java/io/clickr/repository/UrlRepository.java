package io.clickr.repository;

import io.clickr.domain.Url;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlResult;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

/** Reactive repository for URL operations using PostgreSQL. */
@ApplicationScoped
public class UrlRepository {

  private final Pool client;

  @Inject
  public UrlRepository(Pool client) {
    this.client = client;
  }

  private static final String CREATE_QUERY =
      """
          INSERT INTO urls (short_code, original_url, clicks, created_at)
          VALUES ($1, $2, 0, NOW())
          RETURNING id, short_code, original_url, clicks, created_at, last_clicked_at
          """;

  /**
   * Creates a new URL entry in the database.
   *
   * @param originalUrl the original long URL
   * @param shortCode the generated short code
   * @return Uni with the created URL entity
   */
  public Uni<Url> create(String originalUrl, String shortCode) {
    return client
        .preparedQuery(CREATE_QUERY)
        .execute(Tuple.of(shortCode, originalUrl))
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() ? Url.from(iterator.next()) : null);
  }

  private static final String FIND_BY_SHORT_CODE_QUERY =
      """
          SELECT id, short_code, original_url, clicks, created_at, last_clicked_at
          FROM urls
          WHERE short_code = $1
          """;

  /**
   * Finds a URL by its short code.
   *
   * @param shortCode the short code to search for
   * @return Uni with Optional of URL
   */
  public Uni<Optional<Url>> findByShortCode(String shortCode) {
    return client
        .preparedQuery(FIND_BY_SHORT_CODE_QUERY)
        .execute(Tuple.of(shortCode))
        .map(RowSet::iterator)
        .map(
            iterator ->
                iterator.hasNext() ? Optional.of(Url.from(iterator.next())) : Optional.empty());
  }

  private static final String EXISTS_BY_SHORT_CODE_QUERY =
      """
          SELECT EXISTS(SELECT 1 FROM urls WHERE short_code = $1)
          """;

  /**
   * Checks if a short code already exists.
   *
   * @param shortCode the short code to check
   * @return Uni with boolean indicating existence
   */
  public Uni<Boolean> existsByShortCode(String shortCode) {
    return client
        .preparedQuery(EXISTS_BY_SHORT_CODE_QUERY)
        .execute(Tuple.of(shortCode))
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() && iterator.next().getBoolean(0));
  }

  private static final String NEXTVAL_QUERY =
      """
        SELECT nextval('urls_id_seq')
        """;

  /**
   * Gets the next sequence value for generating IDs.
   *
   * @return Uni with the next sequence value
   */
  public Uni<Long> getNextSequenceValue() {
    return client
        .query(NEXTVAL_QUERY)
        .execute()
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() ? iterator.next().getLong(0) : null);
  }

  private static final String DELETE_BY_SHORT_CODE_QUERY =
      """
          DELETE FROM urls WHERE short_code = $1
          """;

  /**
   * Deletes a URL by its short code.
   *
   * @param shortCode the short code of the URL to delete
   * @return Uni with the number of deleted rows
   */
  public Uni<Integer> deleteByShortCode(String shortCode) {
    return client
        .preparedQuery(DELETE_BY_SHORT_CODE_QUERY)
        .execute(Tuple.of(shortCode))
        .map(SqlResult::rowCount);
  }

  private static final String INCREMENT_CLICKS_BY_SHORT_CODE_QUERY =
      """
          UPDATE urls
          SET clicks = clicks + 1,
              last_clicked_at = NOW()
          WHERE short_code = $1
          """;

  /**
   * Increments the click counter and updates the last_clicked_at timestamp atomically.
   *
   * @param shortCode the short code of the URL to increment
   * @return Uni that completes when the update finishes (does not return the updated row)
   */
  public Uni<Void> incrementClickByShortCode(String shortCode) {
    return client
        .preparedQuery(INCREMENT_CLICKS_BY_SHORT_CODE_QUERY)
        .execute(Tuple.of(shortCode))
        .replaceWithVoid();
  }

  private static final String FIND_ORIGINAL_URL_BY_SHORT_CODE_QUERY =
      """
          SELECT original_url
          FROM urls
          WHERE short_code = $1
          """;

  /**
   * Retrieves only the original URL by its short code.
   *
   * @param shortCode the short code to search for
   * @return Uni with Optional containing the original URL string, or empty if not found
   */
  public Uni<Optional<String>> findOriginalUrlByShortCode(String shortCode) {
    return client
        .preparedQuery(FIND_ORIGINAL_URL_BY_SHORT_CODE_QUERY)
        .execute(Tuple.of(shortCode))
        .map(RowSet::iterator)
        .map(
            iterator ->
                iterator.hasNext() ? Optional.of(iterator.next().getString(0)) : Optional.empty());
  }
}
