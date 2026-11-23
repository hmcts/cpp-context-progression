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

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
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
    @Value(key = "courtscheduler.timeout.connect.ms", defaultValue = "5000")
    private String connectTimeoutMs = "5000";

    @Inject
    @Value(key = "courtscheduler.timeout.ms", defaultValue = "10000")
    private String socketTimeoutMs = "10000";

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

            return execute(httpGet);
        } catch (final URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to retrieve court schedules by id", ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    private Response execute(final HttpGet httpGet) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Integer.parseInt(connectTimeoutMs))
                        .setSocketTimeout(Integer.parseInt(socketTimeoutMs))
                        .build())
                .build();
             CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            final String body = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());
            if (statusCode == HttpStatus.SC_OK) {
                return Response
                        .status(HttpStatus.SC_OK)
                        .entity(stringToJsonObjectConverter.convert(body))
                        .build();
            }
            LOGGER.error("Retrieve court schedules by id failed with status code:{}", statusCode);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(body)
                    .build();
        }
    }

    private UUID getUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
