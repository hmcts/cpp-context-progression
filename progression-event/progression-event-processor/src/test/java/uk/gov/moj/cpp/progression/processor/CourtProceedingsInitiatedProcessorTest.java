package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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
    private JsonObject hearingDefendantRequestJson;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Sender sender;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtReferral courtReferral;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() {

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .build();
        final ListHearingRequest listHearingRequest = populateListHearingRequest();
        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class))
                .thenReturn(courtReferral);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(hearingDefendantRequestJson);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(singletonList(listHearingRequest));
        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(List.class),
                any(List.class), any(UUID.class))).thenReturn(listCourtHearing);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.create-hearing-defendant-request")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        this.eventProcessor.handle(jsonEnvelope);
        //then
        verify(progressionService).createProsecutionCases(jsonEnvelope, singletonList(prosecutionCase));
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);
    }

    private ListHearingRequest populateListHearingRequest() {
        return ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .build()))
                .build();
    }

}
