package io.clickr.resource;

import io.clickr.service.RedirectService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** REST Resource for handling short URL redirects. */
@Path("/")
@Tag(name = "URL Redirect", description = "Redirects short URLs to their original destinations")
public class RedirectResource {

  private static final Logger log = LoggerFactory.getLogger(RedirectResource.class);

  private final RedirectService redirectService;

  @Inject
  public RedirectResource(RedirectService redirectService) {
    this.redirectService = redirectService;
  }

  @GET
  @Path("/{shortCode}")
  @Produces(value = {MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Redirect to original URL",
      description =
          "Redirects to the original URL associated with the short code. "
              + "Increments the click counter and updates last_clicked_at timestamp.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "302",
            description = "Redirect to original URL",
            content = @Content(schema = @Schema(hidden = true))),
        @APIResponse(
            responseCode = "400",
            description = "Invalid short code format",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Object.class))),
        @APIResponse(
            responseCode = "404",
            description = "Short code not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Object.class))),
      })
  public Uni<RestResponse<Void>> redirect(@PathParam("shortCode") String shortCode) {
    log.info("GET /{} - Redirecting short code", shortCode);
    return redirectService.redirect(shortCode);
  }
}
