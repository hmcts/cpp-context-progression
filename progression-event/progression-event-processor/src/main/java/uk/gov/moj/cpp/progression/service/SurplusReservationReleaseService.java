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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
 * untouched - that filter, not this caller, is what makes it safe.
 *
 * <p>The clerk's draft keeps the merged-away bookingReference on its result line, and hearing
 * checks every booking a share carries with courtscheduler before it lets the share go ahead. That
 * check passes for a booking released here because the release records reason {@code SURPLUS}:
 * courtscheduler then answers {@code SURPLUS}, which is safe to share, where a booking that is
 * simply gone would answer {@code NOT_FOUND} and block every later share of the hearing. It relies
 * on courtscheduler keeping that last booking-log event however old.
 *
 * <p>Best-effort: a failure here must not fail the share, since the slot then simply stays
 * reserved until the purge. So this logs and returns.
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

    // The initial values are what a test gets when it builds the service without CDI.
    @Inject
    @Value(key = "courtscheduler.timeout.connect.ms", defaultValue = "5000")
    protected String connectTimeoutMs = "5000";

    @Inject
    @Value(key = "courtscheduler.timeout.ms", defaultValue = "10000")
    protected String socketTimeoutMs = "10000";

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

            final int status = execute(httpPost);

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
     *
     * <p>A client per call, closed with its response. It runs inside the hearing-resulted event
     * processor, so without timeouts a courtscheduler that accepts the connection and never
     * answers would hold that thread indefinitely.
     *
     * @return the response status
     */
    int execute(final HttpPost httpPost) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Integer.parseInt(connectTimeoutMs))
                        .setSocketTimeout(Integer.parseInt(socketTimeoutMs))
                        .build())
                .build();
             CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            EntityUtils.consumeQuietly(httpResponse.getEntity());
            return httpResponse.getStatusLine().getStatusCode();
        }
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
