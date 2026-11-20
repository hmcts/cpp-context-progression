package uk.gov.moj.cpp.progression.listing.common.service;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CourtSchedulesByIdService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulesByIdService.class);
    private static final String SESSIONS_RESOURCE = "/sessions";
    private static final String IDS = "ids";
    public static final String COURTSCHEDULER_SEARCH_COURT_SCHEDULES_BY_ID_TYPE = "application/vnd.courtscheduler.search.court-schedules-by-id+json";
    public static final String CJS_CPP_UID = "CJSCPPUID";

    @Inject
    @Value(key = "courtscheduler.base.url", defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    private String baseUri;

    @Inject
    SystemUserProvider systemUserProvider;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response getCourtSchedulesById(final List<String> courtScheduleIds) {
        LOGGER.info("Retrieve court schedules by id '{}'", courtScheduleIds);

        try {
            final URIBuilder uriBuilder = new URIBuilder(baseUri + SESSIONS_RESOURCE)
                    .addParameter(IDS, String.join(",", courtScheduleIds));
            final HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.addHeader(ACCEPT, COURTSCHEDULER_SEARCH_COURT_SCHEDULES_BY_ID_TYPE);
            httpGet.addHeader(CJS_CPP_UID, getUserId().toString());

            final HttpResponse httpResponse = HttpClientBuilder
                    .create()
                    .build()
                    .execute(httpGet);

            if (httpResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
                return Response
                        .status(HttpStatus.SC_OK)
                        .entity(stringToJsonObjectConverter.convert(EntityUtils.toString(httpResponse.getEntity())))
                        .build();
            }
            LOGGER.error("Retrieve court schedules by id failed with status code:{}",
                    httpResponse.getStatusLine().getStatusCode());
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(EntityUtils.toString(httpResponse.getEntity()))
                    .build();
        } catch (final URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to retrieve court schedules by id", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
