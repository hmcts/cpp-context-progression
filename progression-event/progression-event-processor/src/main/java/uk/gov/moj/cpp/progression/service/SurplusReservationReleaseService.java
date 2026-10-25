package uk.gov.moj.cpp.progression.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
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
 * <p>Uses courtscheduler's batch release, {@code POST /unconfirmedBooking/release}, which gives
 * back only holds that still carry an {@code expires_at} and never touches anything keyed by
 * hearing_id. So a bookingId whose result has already been shared leaves its confirmed listing
 * untouched - that filter, not this caller, is what makes it safe. (It used to go through
 * {@code DELETE /sessions/{id}}, the vacated-hearing release, which also releases by hearing_id.)
 *
 * <p>Best-effort: a failure here must not fail the share, since the slot then simply stays
 * reserved until the purge, which is the behaviour we had before. So this logs and returns.
 *
 * <p><b>This class deletes a booking the clerk's draft still references, and the results UI must
 * tolerate that.</b> The draft keeps the merged-away bookingReference on its result line, so the
 * pre-share gate asks courtscheduler for that booking's status and is told {@code NOT_FOUND} -
 * which the gate once read as "the hold expired, refuse the share". Every subsequent share on the
 * hearing was then silently refused: no request sent, nothing logged, the clerk simply could not
 * share again (LPT-2599). The fix is in the UI - {@code getBookingReferencesToCheck} skips a line
 * that has a {@code sharedDate} and no {@code amendmentReason} - not here, because a released
 * booking is genuinely gone and courtscheduler cannot distinguish "expired" from "released as
 * surplus" either way.
 *
 * <p>So the two changes are a pair. Removing this class was tried on 2026-10-15 and reverted:
 * without it {@code @LPT-2582} and {@code @LPT-2599} both leave a stranded
 * {@code RESERVED_UNCONFIRMED} row behind on STE02, because listing and courtscheduler release
 * only what they confirm and nothing else covers the merged-away booking.
 */
@ApplicationScoped
public class SurplusReservationReleaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurplusReservationReleaseService.class);
    private static final String RELEASE_RESOURCE = "/unconfirmedBooking/release";
    private static final String COURTSCHEDULER_RELEASE_UNCONFIRMED_BOOKINGS_TYPE = "application/vnd.courtscheduler.release.unconfirmed.bookings+json";
    private static final String CJS_CPP_UID = "CJSCPPUID";
    private static final String RELEASE_REASON_SURPLUS = "SURPLUS";

    @Inject
    @Value(key = "courtscheduler.base.url", defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    private String baseUri;

    @Inject
    private SystemUserProvider systemUserProvider;

    /** Releases every surplus booking of one share in a single call. Nothing to release, no call. */
    public void release(final Set<UUID> bookingReferences) {
        if (bookingReferences.isEmpty()) {
            return;
        }
        try {
            final JsonArrayBuilder bookingIds = createArrayBuilder();
            bookingReferences.forEach(bookingReference -> bookingIds.add(bookingReference.toString()));

            final HttpPost httpPost = new HttpPost(new URIBuilder(baseUri + RELEASE_RESOURCE).build());
            httpPost.addHeader(CONTENT_TYPE, COURTSCHEDULER_RELEASE_UNCONFIRMED_BOOKINGS_TYPE);
            httpPost.addHeader(CJS_CPP_UID, getUserId().toString());
            // LPT-2611: courtscheduler records why each hold went; these were made surplus by a share.
            httpPost.setEntity(new StringEntity(createObjectBuilder()
                    .add("bookingIds", bookingIds)
                    .add("reason", RELEASE_REASON_SURPLUS)
                    .build().toString(), UTF_8));

            final HttpResponse httpResponse = execute(httpPost);
            final int status = httpResponse.getStatusLine().getStatusCode();

            if (status == Response.Status.ACCEPTED.getStatusCode()) {
                LOGGER.info("Released surplus reservations bookingReferences={}", bookingReferences);
            } else {
                LOGGER.error("Could not release surplus reservations bookingReferences={} status={}",
                        bookingReferences, status);
            }
        } catch (URISyntaxException | IOException | IllegalStateException ex) {
            LOGGER.error("Exception releasing surplus reservations bookingReferences=" + bookingReferences, ex);
        }
    }


    /**
     * The one network call, isolated so a test can stand in for it. Package-private rather than
     * private for that reason only - nothing outside this class should call it.
     */
    HttpResponse execute(final HttpPost httpPost) throws IOException {
        return HttpClientBuilder.create().build().execute(httpPost);
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
