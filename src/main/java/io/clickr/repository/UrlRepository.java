package io.clickr.repository;

import io.clickr.domain.Url;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.RowSet;
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

  /**
   * Creates a new URL entry in the database.
   *
   * @param originalUrl the original long URL
   * @param shortCode the generated short code
   * @return Uni with the created URL entity
   */
  public Uni<Url> create(String originalUrl, String shortCode) {
    return client
        .preparedQuery(
            "INSERT INTO urls (short_code, original_url, clicks, created_at) "
                + "VALUES ($1, $2, 0, NOW()) "
                + "RETURNING id, short_code, original_url, clicks, created_at, last_clicked_at")
        .execute(Tuple.of(shortCode, originalUrl))
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() ? Url.from(iterator.next()) : null);
  }

  /**
   * Finds a URL by its short code.
   *
   * @param shortCode the short code to search for
   * @return Uni with Optional of URL
   */
  public Uni<Optional<Url>> findByShortCode(String shortCode) {
    return client
        .preparedQuery(
            "SELECT id, short_code, original_url, clicks, created_at, last_clicked_at "
                + "FROM urls WHERE short_code = $1")
        .execute(Tuple.of(shortCode))
        .map(RowSet::iterator)
        .map(
            iterator ->
                iterator.hasNext() ? Optional.of(Url.from(iterator.next())) : Optional.empty());
  }

  /**
   * Checks if a short code already exists.
   *
   * @param shortCode the short code to check
   * @return Uni with boolean indicating existence
   */
  public Uni<Boolean> existsByShortCode(String shortCode) {
    return client
        .preparedQuery("SELECT EXISTS(SELECT 1 FROM urls WHERE short_code = $1)")
        .execute(Tuple.of(shortCode))
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() && iterator.next().getBoolean(0));
  }

  /**
   * Gets the next sequence value for generating IDs.
   *
   * @return Uni with the next sequence value
   */
  public Uni<Long> getNextSequenceValue() {
    return client
        .query("SELECT nextval('urls_id_seq')")
        .execute()
        .map(RowSet::iterator)
        .map(iterator -> iterator.hasNext() ? iterator.next().getLong(0) : null);
  }

  /**
   * Deletes a URL by its short code.
   *
   * @param shortCode the short code of the URL to delete
   * @return Uni with the number of deleted rows
   */
  public Uni<Integer> deleteByShortCode(String shortCode) {
    return client
        .preparedQuery("DELETE FROM urls WHERE short_code = $1")
        .execute(Tuple.of(shortCode))
        .map(rowSet -> rowSet.rowCount());
  }
}
