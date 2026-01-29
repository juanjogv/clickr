package io.clickr.resource;

import io.clickr.domain.dto.CreateUrlRequest;
import io.clickr.domain.dto.UrlResponse;
import io.clickr.service.UrlService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** REST Resource for URL shortening operations. */
@Path("/api/v1/urls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "URL Shortening", description = "Operations for creating and managing short URLs")
public class UrlResource {

  private static final Logger log = LoggerFactory.getLogger(UrlResource.class);

  private final UrlService urlService;

  @Inject
  public UrlResource(UrlService urlService) {
    this.urlService = urlService;
  }

  @POST
  @Operation(
      summary = "Create a short URL",
      description = "Creates a new short URL from a long URL")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "201", description = "Short URL created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid URL provided"),
        @APIResponse(responseCode = "500", description = "Internal server error")
      })
  public Uni<RestResponse<UrlResponse>> createShortUrl(
      @Valid CreateUrlRequest request, @Context UriInfo uriInfo) {
    log.trace("POST /api/v1/urls - Creating short URL for: {}", request.url());

    return urlService
        .createShortUrl(request)
        .map(
            urlResponse ->
                RestResponse.created(
                    uriInfo.getAbsolutePathBuilder().path(urlResponse.shortCode()).build()));
  }

  @GET
  @Path("/{shortCode}")
  @Operation(
      summary = "Get URL information",
      description = "Retrieves information about a short URL")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "URL found",
            content = @Content(schema = @Schema(implementation = UrlResponse.class))),
        @APIResponse(responseCode = "404", description = "Short code not found")
      })
  public Uni<Response> getUrl(@PathParam("shortCode") String shortCode) {
    log.trace("GET /api/v1/urls/{} - Retrieving URL info", shortCode);

    return urlService
        .getUrlByShortCode(shortCode)
        .map(
            optionalUrl ->
                optionalUrl
                    .map(url -> Response.ok(url).build())
                    .orElse(
                        Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Short code not found: " + shortCode))
                            .build()));
  }

  @DELETE
  @Path("/{shortCode}")
  @Operation(summary = "Delete a short URL", description = "Deletes a short URL by its code")
  @APIResponses(
      value = {
        @APIResponse(responseCode = "204", description = "URL deleted successfully"),
        @APIResponse(responseCode = "404", description = "Short code not found")
      })
  public Uni<Response> deleteUrl(@PathParam("shortCode") String shortCode) {
    log.trace("DELETE /api/v1/urls/{} - Deleting short URL", shortCode);

    return urlService
        .deleteUrl(shortCode)
        .map(
            deleted ->
                deleted
                    ? Response.noContent().build()
                    : Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Short code not found: " + shortCode))
                        .build());
  }

  /** Simple error response DTO. */
  public static class ErrorResponse {
    private String error;

    public ErrorResponse() {}

    public ErrorResponse(String error) {
      this.error = error;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }
  }
}
