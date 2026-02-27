package uk.gov.justice.api.resource;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.api.resource.utils.FileUtil;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.service.CourtlistQueryService;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for GET /courtlistdata (standard and prison list data as JSON).
 */
@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiCourtlistdataResourceTest {

    private static final String COURT_LIST_DATA_QUERY_NAME = "progression.search.court.list.data";
    private static final String PRISON_COURT_LIST = "PRISON";
    private static final String PRISON_COURT_LIST_DATA_QUERY_NAME = "progression.search.prison.court.list.data";

    private final UUID userId = UUID.randomUUID();
    private final String courtCentreId = UUID.randomUUID().toString();
    private final String courtRoomId = UUID.randomUUID().toString();
    private final String listId = "PUBLIC";
    private final String startDate = STRING.next();
    private final String endDate = STRING.next();

    @Mock
    private CourtlistQueryService courtlistQueryService;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @InjectMocks
    private DefaultQueryApiCourtlistdataResource defaultQueryApiCourtlistdataResource;

    @Test
    public void shouldReturnCourtListDataAsJsonWhenGetCourtlistdata() {
        final JsonEnvelope interceptorResponse = envelopeFrom(
                metadataWithRandomUUID(COURT_LIST_DATA_QUERY_NAME),
                FileUtil.jsonFromPath("stub-data/progression.search.court.list.json"));
        final JsonObject enrichedPayload = FileUtil.jsonFromPath("stub-data/stagingpubhub.command.publish-standard-list.json");

        when(courtlistQueryService.buildCourtlistQueryEnvelope(any(), any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(COURT_LIST_DATA_QUERY_NAME), FileUtil.jsonFromPath("stub-data/progression.search.court.list.json")));
        when(interceptorChainProcessor.process(any())).thenReturn(of(interceptorResponse));
        when(courtlistQueryService.buildEnrichedPayload(interceptorResponse))
                .thenReturn(enrichedPayload);

        final Response response = defaultQueryApiCourtlistdataResource.getCourtlistdata(
                courtCentreId, courtRoomId, listId, startDate, endDate, false, userId);

        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
        assertEquals(enrichedPayload, response.getEntity());

        verify(courtlistQueryService).buildCourtlistQueryEnvelope(
                eq(courtCentreId), eq(courtRoomId), eq(listId), eq(startDate), eq(endDate),
                eq(false), eq(userId), eq(COURT_LIST_DATA_QUERY_NAME));
        verify(interceptorChainProcessor).process(org.mockito.ArgumentMatchers.any());
        verify(courtlistQueryService).buildEnrichedPayload(interceptorResponse);
    }

    @Test
    public void shouldReturnForbiddenWhenGetCourtlistdataWithPrisonListId() {
        final Response response = defaultQueryApiCourtlistdataResource.getCourtlistdata(
                courtCentreId, courtRoomId, "PRISON", startDate, endDate, false, userId);

        assertThat(response.getStatus(), is(FORBIDDEN.getStatusCode()));
    }

    @Test
    public void shouldReturnPrisonCourtListDataAsJsonWhenGetPrisonCourtlistdata() {
        final JsonEnvelope interceptorResponse = envelopeFrom(
                metadataWithRandomUUID(PRISON_COURT_LIST_DATA_QUERY_NAME),
                FileUtil.jsonFromPath("stub-data/progression.search.usher.list.json"));
        final JsonObject enrichedPayload = FileUtil.jsonFromPath("stub-data/stagingpubhub.command.ushers-standard-list.json");

        when(courtlistQueryService.buildCourtlistQueryEnvelope(any(), any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(PRISON_COURT_LIST_DATA_QUERY_NAME), FileUtil.jsonFromPath("stub-data/progression.search.usher.list.json")));
        when(interceptorChainProcessor.process(any())).thenReturn(of(interceptorResponse));
        when(courtlistQueryService.buildEnrichedPayload(interceptorResponse))
                .thenReturn(enrichedPayload);

        final Response response = defaultQueryApiCourtlistdataResource.getPrisonCourtlistdata(
                courtCentreId, courtRoomId, startDate, endDate, userId);

        assertThat(response.getStatus(), is(SC_OK));
        assertThat(response.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
        assertEquals(enrichedPayload, response.getEntity());

        verify(courtlistQueryService).buildCourtlistQueryEnvelope(
                eq(courtCentreId), eq(courtRoomId), eq(PRISON_COURT_LIST), eq(startDate), eq(endDate),
                eq(false), eq(userId), eq(PRISON_COURT_LIST_DATA_QUERY_NAME));
        verify(interceptorChainProcessor).process(org.mockito.ArgumentMatchers.any());
        verify(courtlistQueryService).buildEnrichedPayload(interceptorResponse);
    }
}
