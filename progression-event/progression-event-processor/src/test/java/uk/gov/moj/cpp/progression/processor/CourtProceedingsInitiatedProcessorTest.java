package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtProceedingsInitiatedProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @InjectMocks
    private CourtProceedingsInitiatedProcessor eventProcessor;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObject courtReferralJson;

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtReferral courtReferral;

    @Spy
    private ProgressionService progressionService;

    @Spy
    private ListingService listingService;

    @Spy
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.progressionService, "objectToJsonObjectConverter", this.objectToJsonObjectConverter);
        setField(this.progressionService, "enveloper", this.enveloper);
        setField(this.progressionService, "sender", this.sender);
        setField(this.listingService, "objectToJsonObjectConverter", this.objectToJsonObjectConverter);
        setField(this.listingService, "sender", this.sender);
        setField(this.listingService, "enveloper", this.enveloper);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(Offence.offence().withId(offenceId).build()))
                        .build()))
                .build();
        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(singletonList(listHearingRequest));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);
        //When
        this.eventProcessor.handle(requestMessage);

        verify(sender, times(4)).send(envelopeCaptor.capture());

        assertThat("progression.command.create-hearing-defendant-request", is(envelopeCaptor.getAllValues().get(0).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(0).payload().getJsonArray("defendantRequests").getJsonObject(0).getString("prosecutionCaseId")));

        assertThat("progression.command.create-prosecution-case", is(envelopeCaptor.getAllValues().get(1).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase").getString("id")));

        assertThat("progression.command.update-defendant-listing-status", is(envelopeCaptor.getAllValues().get(2).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(2).payload().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id")));
        assertThat("SENT_FOR_LISTING", is(envelopeCaptor.getAllValues().get(2).payload().getString("hearingListingStatus")));

        assertThat("listing.command.list-court-hearing", is(envelopeCaptor.getAllValues().get(3).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(3).payload().getJsonArray("hearings").getJsonObject(0).getJsonArray("prosecutionCases").getJsonObject(0).getString("id")));
    }

    private ListHearingRequest populateListHearingRequest(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .build()))
                .build();
    }

}
