package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.COURT_LIST_QUERY_NAME;
import static uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource.DISPOSITION;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between docmosis and progression context. Class invoke standard interceptor chain. At
 * the end of interceptor chain, regular query handler is invoked and returns documents details
 */

@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryApiCourtlistResourceTest {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
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
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;
    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;
    @Mock
    private DocumentGeneratorClient documentGeneratorClient;
    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;
    @InjectMocks
    private DefaultQueryApiCourtlistResource defaultQueryApiCourtlistResource;

    @Before
    public void init() {
        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocument() throws IOException {
        final String pdfContent = "PDF Content";
        final JsonEnvelope interceptorResponse = documentDetails();
        final byte[] documentGeneratorClientResponse = pdfContent.getBytes();

        final MultivaluedMap headers = new MultivaluedHashMap(ImmutableMap.of(CONTENT_TYPE, PDF_CONTENT_TYPE, CONTENT_DISPOSITION, DISPOSITION));

        when(interceptorChainProcessor.process(argThat((any(InterceptorContext.class))))).thenReturn(ofNullable(interceptorResponse));
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(eq(interceptorResponse.payloadAsJsonObject()), anyString(), eq(systemUserId)))
                .thenReturn(documentGeneratorClientResponse);

        final Response actual = defaultQueryApiCourtlistResource
                .getCourtlist(courtCentreId.toString(), courtRoomId.toString(), listId.toString(),
                        startDate, endDate, false, userId);
        final InputStream inputStream = (InputStream) actual.getEntity();

        assertThat(actual.getStatus(), is(SC_OK));
        assertThat(actual.getHeaders(), is(headers));
        assertThat(pdfContent.getBytes(), is(IOUtils.toByteArray(inputStream)));
        verifyInterceptorChainExecution();
    }

    @Test
    public void shouldOverrideGeneratedDefaultAdapterClass() {
        assertThat(defaultQueryApiCourtlistResource.getClass().getName(),
                is("uk.gov.justice.api.resource.DefaultQueryApiCourtlistResource"));
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

    private JsonEnvelope documentDetails() {
        final JsonObject payload = createObjectBuilder()
                .add("templateName", "templateName")
                .build();
        return envelopeFrom(metadataWithRandomUUID(COURT_LIST_QUERY_NAME), payload);
    }
}