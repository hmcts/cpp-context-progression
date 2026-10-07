package uk.gov.moj.cpp.progression.command.service;



import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseQueryServiceTest {

    @Mock
    private Requester requester;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> adminEnvelopeCaptor;

    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";
    private static final String PROGRESSION_QUERY_COURT_PROCEEDINGS_FOR_APPLICATION = "progression.query.court-proceedings-for-application";

    @Test
    public void shouldRequestForProsecutionCase() {
        final String caseId = UUID.randomUUID().toString();
        final JsonObject sampleJsonObject = createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                        .add("id", caseId)
                        .add("prosecutionCaseIdentifier", createObjectBuilder()
                                .add("caseURN" , "case123")
                                .add("prosecutionAuthorityReference", "ref")
                                .add("prosecutionAuthorityOUCode", "ouCode")
                                .build())
                        .build())
                .build();

        when(requester.request(any()))
                .thenReturn(JsonEnvelopeBuilder.envelope().withPayloadFrom(sampleJsonObject).with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES)).build());

        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES))
                .build();
        final Optional<JsonObject> result = prosecutionCaseQueryService.getProsecutionCase(
                envelope, UUID.randomUUID());

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(result.get().getJsonObject("prosecutionCase").getString("id"), is(caseId));

        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldReturnCourtApplicationFromCourtProceedingsForApplication() {
        final UUID applicationId = randomUUID();
        final UUID courtOrderId = randomUUID();
        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class)))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(PROGRESSION_QUERY_COURT_PROCEEDINGS_FOR_APPLICATION),
                        createObjectBuilder().add("courtApplication", createObjectBuilder()
                                .add("id", applicationId.toString())
                                .add("courtOrder", createObjectBuilder().add("id", courtOrderId.toString()))).build()));

        final Optional<CourtApplication> result = prosecutionCaseQueryService.getCourtProceedingsForApplication(applicationId, commandEnvelope());

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getId(), is(applicationId));
        assertThat(result.get().getCourtOrder(), is(notNullValue()));
        assertThat(result.get().getCourtOrder().getId(), is(courtOrderId));
        assertThat(result.get().getParentApplicationId(), is(nullValue()));
        verify(requester).requestAsAdmin(adminEnvelopeCaptor.capture(), eq(JsonObject.class));
        assertThat(adminEnvelopeCaptor.getValue().metadata().name(), is(PROGRESSION_QUERY_COURT_PROCEEDINGS_FOR_APPLICATION));
        assertThat(adminEnvelopeCaptor.getValue().payload().getString("applicationId"), is(applicationId.toString()));
    }

    @Test
    public void shouldReturnEmptyWhenCourtProceedingsForApplicationHasNoCourtApplication() {
        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class)))
                .thenReturn(envelopeFrom(metadataWithRandomUUID(PROGRESSION_QUERY_COURT_PROCEEDINGS_FOR_APPLICATION), createObjectBuilder().build()));

        assertThat(prosecutionCaseQueryService.getCourtProceedingsForApplication(randomUUID(), commandEnvelope()).isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyWhenCourtProceedingsForApplicationLookupFails() {
        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class))).thenThrow(new NullPointerException());

        assertThat(prosecutionCaseQueryService.getCourtProceedingsForApplication(randomUUID(), commandEnvelope()).isPresent(), is(false));
    }

    private JsonEnvelope commandEnvelope() {
        return envelope().with(metadataWithRandomUUID("progression.initiate-court-proceedings-for-application")).build();
    }
}
