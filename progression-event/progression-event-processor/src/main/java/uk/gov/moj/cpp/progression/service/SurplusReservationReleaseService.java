package uk.gov.moj.cpp.progression.service;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gives back a reservation that a share resolved but did not list against.
 *
 * <p>The results UI reserves a slot per result line, but the share merges every next hearing that
 * resolves to one session into a single listing need - correctly, since that is one hearing in one
 * session. The reservations behind the lines that were merged away are then surplus: nothing lists
 * against them, so nothing consumes them, and the hearing-keyed release cannot see them because a
 * reservation carries no hearing_id. Left alone they hold a slot of that session until the
 * overnight purge, counting against capacity exactly like a real listing.
 *
 * <p>Hits the same {@code DELETE /sessions/{id}} courtscheduler exposes for a vacated hearing. The
 * booking-scoped half of that endpoint removes only rows that still carry an {@code expires_at},
 * so a bookingId whose result has already been shared leaves its confirmed listing untouched -
 * that filter, not this caller, is what makes it safe.
 *
 * <p>Best-effort: a failure here must not fail the share, since the slot then simply stays
 * reserved until the purge, which is the behaviour we had before. So this logs and returns.
 */
@ApplicationScoped
public class SurplusReservationReleaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurplusReservationReleaseService.class);
    private static final String SESSIONS_RESOURCE = "/sessions";
    private static final String COURTSCHEDULER_RELEASE_SESSIONS_TYPE = "application/vnd.courtscheduler.release.sessions+json";
    private static final String CJS_CPP_UID = "CJSCPPUID";

    @Inject
    @Value(key = "courtscheduler.base.url", defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    private String baseUri;

    @Inject
    private SystemUserProvider systemUserProvider;

    public void release(final UUID bookingReference) {
        try {
            final HttpDelete httpDelete = new HttpDelete(
                    new URIBuilder(baseUri + SESSIONS_RESOURCE + "/" + bookingReference).build());
            httpDelete.addHeader(CONTENT_TYPE, COURTSCHEDULER_RELEASE_SESSIONS_TYPE);
            httpDelete.addHeader(CJS_CPP_UID, getUserId().toString());

            final HttpResponse httpResponse = execute(httpDelete);
            final int status = httpResponse.getStatusLine().getStatusCode();

            if (status == Response.Status.ACCEPTED.getStatusCode()) {
                LOGGER.info("Released surplus reservation bookingReference={}", bookingReference);
            } else {
                LOGGER.error("Could not release surplus reservation bookingReference={} status={}",
                        bookingReference, status);
            }
        } catch (URISyntaxException | IOException | IllegalStateException ex) {
            LOGGER.error("Exception releasing surplus reservation bookingReference=" + bookingReference, ex);
        }
    }


    /**
     * The one network call, isolated so a test can stand in for it. Package-private rather than
     * private for that reason only - nothing outside this class should call it.
     */
    HttpResponse execute(final HttpDelete httpDelete) throws IOException {
        return HttpClientBuilder.create().build().execute(httpDelete);
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
