package uk.gov.moj.cpp.progression.service;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.progression.exception.DataValidationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Progression's own client for the court scheduler's provisional booking endpoint, replacing the one
 * it used to get from listing-common.
 *
 * listing-common is a Java dependency on another context, which forces a release order, and its only
 * published build is compiled against javax - its methods return javax.ws.rs.core.Response, which
 * does not exist on the Jakarta stack. What progression actually depends on is the court scheduler's
 * HTTP contract, and that is what this calls: same base URL property, same resource path, same accept
 * media type and CJSCPPUID header, same status handling.
 */
@ApplicationScoped
public class ProvisionalBookingService {

    public static final String COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE =
            "application/vnd.courtscheduler.get.provisional.booking+json";
    public static final String CJS_CPP_UID = "CJSCPPUID";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionalBookingService.class);
    private static final String PROVISIONAL_RESOURCE = "/provisionalBooking";

    @Inject
    @Value(key = "courtscheduler.base.url",
            defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    private String baseUri;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response getSlots(final Map<String, String> params) {

        if (LOGGER.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) -> LOGGER.info("Retrieve ProvisionalSlots in S & L with params '{}-{}'", key, value));
        }

        if (params == null) {
            throw new DataValidationException("Params for search ProvisionalSlots is null ....");
        }

        try {
            final HttpGet httpGet = new HttpGet(new URL(baseUri + PROVISIONAL_RESOURCE).toString());
            httpGet.addHeader(ACCEPT, COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE);
            httpGet.addHeader(CJS_CPP_UID, getUserId().toString());

            final URIBuilder uriBuilder = new URIBuilder(httpGet.getURI());
            params.forEach(uriBuilder::addParameter);
            httpGet.setURI(uriBuilder.build());

            final HttpResponse httpResponse = httpClient().execute(httpGet);

            if (httpResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Retrieve ProvisionalSlots successfully");
                }
                return Response
                        .status(HttpStatus.SC_OK)
                        .entity(stringToJsonObjectConverter.convert(EntityUtils.toString(httpResponse.getEntity())))
                        .build();
            }

            LOGGER.error("Retrieve ProvisionalSlots failed with status code:{}",
                    httpResponse.getStatusLine().getStatusCode());
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(httpResponse.getEntity())
                    .build();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Retrieve Provisional Booking Slots", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    /**
     * Package-private so a test can substitute the client without a running court scheduler.
     * Mirrors MaterialClient, the other service this decoupling brought into progression.
     */
    HttpClient httpClient() {
        return HttpClientBuilder.create().build();
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
