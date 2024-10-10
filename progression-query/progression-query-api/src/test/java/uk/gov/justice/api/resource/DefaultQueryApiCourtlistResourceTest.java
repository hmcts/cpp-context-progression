package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.COURT_LIST_QUERY_NAME;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.PDF_DISPOSITION;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.PRISON_COURT_LIST;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.WORD_DISPOSITION;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.api.resource.service.StagingPubHubService;
import uk.gov.justice.api.resource.utils.FileUtil;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between docmosis and progression context. Class invoke standard interceptor chain. At
 * the end of interceptor chain, regular query handler is invoked and returns documents details
 */

@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiCourtlistResourceTest {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String WORD_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private final UUID userId = randomUUID();
    private final UUID systemUserId = randomUUID();
    private final UUID courtCentreId = randomUUID();
    private final UUID courtRoomId = randomUUID();
    private final UUID listId = randomUUID();
    private final String startDate = STRING.next();
    private final String endDate = STRING.next();

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private StagingPubHubService stagingPubHubService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;

    @InjectMocks
    private DefaultQueryApiCourtlistResource defaultQueryApiCourtlistResource;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocument() throws IOException {
        final String pdfContent = "PDF Content";
        final JsonEnvelope interceptorResponse = envelopeFrom(metadataWithRandomUUID(COURT_LIST_QUERY_NAME),
                FileUtil.jsonFromPath("stub-data/progression.search.court.list.json"));
        final byte[] documentGeneratorClientResponse = pdfContent.getBytes();

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>(ImmutableMap.of(CONTENT_TYPE, PDF_CONTENT_TYPE, CONTENT_DISPOSITION, PDF_DISPOSITION));


        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(ofNullable(interceptorResponse));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        assert interceptorResponse != null;
        when(documentGeneratorClient.generatePdfDocument(eq(interceptorResponse.payloadAsJsonObject()), anyString(), eq(systemUserId)))
                .thenReturn(documentGeneratorClientResponse);
        when(referenceDataService.getCourtCenterDataByCourtName(argThat(any(JsonEnvelope.class)), anyString()))
                .thenReturn(of(FileUtil.jsonFromPath("stub-data/referencedata.query.ou.courtrooms.ou-courtroom-name.json")));


        final Response actual = defaultQueryApiCourtlistResource
                .getCourtlist(courtCentreId.toString(), courtRoomId.toString(), listId.toString(),
                        startDate, endDate, false, userId);
        final InputStream inputStream = (InputStream) actual.getEntity();

        assertThat(actual.getStatus(), is(SC_OK));
        assertThat(actual.getHeaders(), is(headers));
        assertThat(pdfContent.getBytes(), is(IOUtils.toByteArray(inputStream)));
        verifyInterceptorChainExecution();

        verify(referenceDataService).getCourtCenterDataByCourtName(envelopeArgumentCaptor.capture(), stringArgumentCaptor.capture());
        assertEquals("Lavender Hill Magistrates' Court", stringArgumentCaptor.getValue());
        assertEquals(interceptorResponse.payloadAsJsonObject(), envelopeArgumentCaptor.getValue().payloadAsJsonObject());

        verify(stagingPubHubService).publishStandardList(jsonObjectArgumentCaptor.capture(), uuidArgumentCaptor.capture());

        final JsonObject expectedJson = FileUtil.jsonFromPath("stub-data/stagingpubhub.command.publish-standard-list.json");
        assertEquals(userId, uuidArgumentCaptor.getValue());
        assertEquals(expectedJson, jsonObjectArgumentCaptor.getValue());
    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocumentForWord() throws IOException {
        final String wordContent = "Word Content";
        final JsonEnvelope interceptorResponse = envelopeFrom(metadataWithRandomUUID(COURT_LIST_QUERY_NAME),
                FileUtil.jsonFromPath("stub-data/progression.search.usher.list.json"));
        final byte[] documentGeneratorClientResponse = wordContent.getBytes();

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>(ImmutableMap.of(CONTENT_TYPE, WORD_CONTENT_TYPE, CONTENT_DISPOSITION, WORD_DISPOSITION));

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(ofNullable(interceptorResponse));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        assert interceptorResponse != null;
        when(documentGeneratorClient.generateWordDocument(eq(interceptorResponse.payloadAsJsonObject()), anyString(), eq(systemUserId)))
                .thenReturn(documentGeneratorClientResponse);
        when(referenceDataService.getCourtCenterDataByCourtName(argThat(any(JsonEnvelope.class)), anyString()))
                .thenReturn(of(FileUtil.jsonFromPath("stub-data/referencedata.query.ou.courtrooms.ou-courtroom-name.json")));


        final Response actual = defaultQueryApiCourtlistResource
                .getCourtlist(courtCentreId.toString(), courtRoomId.toString(), listId.toString(),
                        startDate, endDate, false, userId);
        final InputStream inputStream = (InputStream) actual.getEntity();

        assertThat(actual.getStatus(), is(SC_OK));
        assertThat(actual.getHeaders(), is(headers));
        assertThat(wordContent.getBytes(), is(IOUtils.toByteArray(inputStream)));
        verifyInterceptorChainExecution();

        verify(referenceDataService).getCourtCenterDataByCourtName(envelopeArgumentCaptor.capture(), stringArgumentCaptor.capture());
        assertEquals("Lavender Hill Magistrates' Court", stringArgumentCaptor.getValue());
        assertEquals(interceptorResponse.payloadAsJsonObject(), envelopeArgumentCaptor.getValue().payloadAsJsonObject());

        verify(stagingPubHubService).publishStandardList(jsonObjectArgumentCaptor.capture(), uuidArgumentCaptor.capture());

        final JsonObject expectedJson = FileUtil.jsonFromPath("stub-data/stagingpubhub.command.ushers-standard-list.json");
        assertEquals(userId, uuidArgumentCaptor.getValue());
        assertEquals(expectedJson, jsonObjectArgumentCaptor.getValue());
    }

    @Test
    public void shouldOverrideGeneratedDefaultAdapterClass() {
        assertThat(defaultQueryApiCourtlistResource.getClass().getName(),
                is("uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource"));
    }

    @Test
    public void shouldErrorWhenCourtListCalledForPrisonList() {
        final Response courtlistResponse = defaultQueryApiCourtlistResource
                .getCourtlist(courtCentreId.toString(), courtRoomId.toString(), PRISON_COURT_LIST,
                        startDate, endDate, false, userId);
        assertThat(courtlistResponse.getStatus(), is(FORBIDDEN.getStatusCode()));
    }

    private void verifyInterceptorChainExecution() {
        verify(interceptorChainProcessor).process(interceptorContextCaptor.capture());

        assertThat(interceptorContextCaptor.getValue().inputEnvelope(), jsonEnvelope(metadata()
                        .withName(COURT_LIST_QUERY_NAME)
                        .withUserId(userId.toString()),
                payload().isJson(allOf(
                        withJsonPath("$.courtCentreId", equalTo(courtCentreId.toString())),
                        withJsonPath("$.listId", equalTo(listId.toString())),
                        withJsonPath("$.courtRoomId", equalTo(courtRoomId.toString())),
                        withJsonPath("$.startDate", equalTo(startDate)),
                        withJsonPath("$.endDate", equalTo(endDate)),
                        withJsonPath("$.restricted", equalTo(false))
                ))
        ));
    }

}