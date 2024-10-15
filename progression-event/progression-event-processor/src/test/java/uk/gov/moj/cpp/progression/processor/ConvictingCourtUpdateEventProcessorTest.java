package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConvictingCourtUpdateEventProcessorTest {

    public static final String JSON_ASSOCIATED_ORGANISATION_JSON = "public.sjp.events.conviction-court-resolved.json";

    final private String caseID = "335306b7-490e-428f-9523-de07a99ac8aa";
    final private String offenseID = "2527c5ac-6861-44f2-bd96-40966f7e3236";
    final private String ouCode = "B01FA00";
    final private UUID courtCenterId = UUID.randomUUID();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    private Sender sender;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @Mock
    private ProgressionService progressionService;

    @Mock
     JsonObject prosecutionCaseDetail;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private ConvictingCourtUpdateEventProcessor convictingCourtUpdateEventProcessor;


    @Test
    public void shouldCallAddConvictingCourtCommand(){

        final JsonObject jsonObjectPayload = readJson(JSON_ASSOCIATED_ORGANISATION_JSON, JsonObject.class);
        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("public.sjp.events.conviction-court-resolved")
                .withId(randomUUID())
                .build();
        JsonEnvelope envelope = envelopeFrom(eventEnvelopeMetadata, jsonObjectPayload);

        when(referenceDataService.getCourtByCourtHouseOUCode(ouCode, envelope, requester)).thenReturn(CourtCentre.courtCentre().withId(courtCenterId).withCode(ouCode).build());
        when(progressionService.getProsecutionCaseDetailById(envelope, caseID)).thenReturn(Optional.of(prosecutionCaseDetail));
        convictingCourtUpdateEventProcessor.handleConvictingCourt(envelope);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.add-convicting-court"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.caseId", equalTo(caseID)),
                withJsonPath("$.addConvictingInformation[0].offenceId", equalTo(offenseID)),
                withJsonPath("$.addConvictingInformation[0].convictingCourt.code", equalTo(ouCode))
        )));
    }

    @Test
    public void callAddConvictingCourtCommandWithoutProsecutionCaseDetail(){

        final JsonObject jsonObjectPayload = readJson(JSON_ASSOCIATED_ORGANISATION_JSON, JsonObject.class);
        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("public.sjp.events.conviction-court-resolved")
                .withId(randomUUID())
                .build();
        JsonEnvelope envelope = envelopeFrom(eventEnvelopeMetadata, jsonObjectPayload);

        when(referenceDataService.getCourtByCourtHouseOUCode(ouCode, envelope, requester)).thenReturn(CourtCentre.courtCentre().withId(courtCenterId).withCode(ouCode).build());
        when(progressionService.getProsecutionCaseDetailById(envelope, caseID)).thenReturn(Optional.empty());
        convictingCourtUpdateEventProcessor.handleConvictingCourt(envelope);

        verify(this.sender, never()).send(this.senderJsonEnvelopeCaptor.capture());

    }

    public <T> T readJson(final String jsonPath, final Class<T> clazz) {
        try {
            final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

            return OBJECT_MAPPER.readValue(getResource(jsonPath), clazz);
        } catch (final IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible: " + e.getMessage());
        }
    }

}