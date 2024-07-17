package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.HearingLanguage.ENGLISH;
import static uk.gov.justice.core.courts.HearingLanguage.WELSH;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredHearingType;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.ReferringJudicialDecision;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.MessageService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.disqualificationreferral.ReferralDisqualifyWarningGenerationService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;
import uk.gov.moj.cpp.progression.transformer.ReferredCourtDocumentTransformer;
import uk.gov.moj.cpp.progression.transformer.ReferredProsecutionCaseTransformer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
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
@SuppressWarnings({"squid:S1607"})
public class CaseReferredToCourtEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    private final UUID prosecutionCaseId = UUID.randomUUID();
    private final UUID offenceId = UUID.randomUUID();
    private final UUID defendantId = UUID.randomUUID();
    private final UUID referralReasonId = UUID.randomUUID();

    @InjectMocks
    private CasesReferredToCourtProcessor eventProcessor;

    @Mock
    private ReferredProsecutionCaseTransformer referredProsecutionCaseTransformer;

    @Mock
    private ReferredCourtDocumentTransformer referredCourtDocumentTransformer;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private MessageService messageService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObject courtReferralJson;

    @Mock
    private JsonObject referralReasonsJson;

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
    private ListingService listingService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private SummonsHearingRequestService summonsHearingRequestService;

    @Mock
    private ReferralDisqualifyWarningGenerationService referralDisqualifyWarningGenerationService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() throws Exception {
        // Setup
        final SjpCourtReferral sjpCourtReferral = getCourtReferral(false, false, WELSH);

        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        final CourtDocument courtDocument = CourtDocument.courtDocument().build();

        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().build()));
        when(progressionService.getReferralReasonByReferralReasonId(any(), any()))
                .thenReturn(Json.createObjectBuilder().add("reason", "reason for referral").build());
        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any(HearingLanguage.class),
                any(JsonEnvelope.class))).thenReturn(prosecutionCase);
        when(referredCourtDocumentTransformer.transform(any(ReferredCourtDocument.class), any
                (JsonEnvelope.class))).thenReturn(courtDocument);
        when(listCourtHearingTransformer.transform(any(), any(), any(), any(), any(UUID.class))).thenReturn
                (listCourtHearing);


        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command" +
                ".create-prosecution-case")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.create-hearing-defendant-request")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.cases-referred-to-court")
                .withUserId(randomUUID().toString()).build());

        //When
        this.eventProcessor.process(jsonEnvelope);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);
        verify(progressionService).createProsecutionCases(any(), any());
        verify(progressionService).createCourtDocument(any(), any());
        verify(summonsHearingRequestService).addDefendantRequestToHearing(eq(jsonEnvelope), any(), any(UUID.class));
        verify(referredProsecutionCaseTransformer).transform(any(ReferredProsecutionCase.class), eq(WELSH), eq(jsonEnvelope));

    }

    @Test
    public void shouldHandleExceptionsOnMissingRequiredData() throws Exception {
        // Setup
        final SjpCourtReferral sjpCourtReferral = getCourtReferral(false, false, WELSH);
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any(HearingLanguage.class), any
                (JsonEnvelope.class))).thenThrow(new MissingRequiredFieldException("value"));

        //When
        this.eventProcessor.process(jsonEnvelope);

        verify(messageService).sendMessage(any(JsonEnvelope.class), any(JsonObject.class), any(String.class));
        verifyNoMoreInteractions(referredCourtDocumentTransformer);
        verifyNoMoreInteractions(listCourtHearingTransformer);
        verifyNoMoreInteractions(referredProsecutionCaseTransformer);

    }

    @Test
    public void shouldHandleExceptionsOnRefData() throws Exception {
        // Setup
        final SjpCourtReferral sjpCourtReferral = getCourtReferral(false, false, ENGLISH);
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any(HearingLanguage.class), any
                (JsonEnvelope.class))).thenThrow(new ReferenceDataNotFoundException("Key", "value"));


        //When
        this.eventProcessor.process(jsonEnvelope);

        verify(messageService).sendMessage(any(JsonEnvelope.class), any(JsonObject.class), any(String.class));
        verifyNoMoreInteractions(referredCourtDocumentTransformer);
        verifyNoMoreInteractions(listCourtHearingTransformer);
        verifyNoMoreInteractions(referredProsecutionCaseTransformer);

    }

    @Test
    public void shouldHandleExceptionsOnSearch() throws Exception {
        // Setup
        final SjpCourtReferral sjpCourtReferral = getCourtReferral(false, false, ENGLISH);
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        //When
        this.eventProcessor.process(jsonEnvelope);

        verify(messageService).sendMessage(any(JsonEnvelope.class), any(JsonObject.class), any(String.class));
        verifyNoMoreInteractions(referredProsecutionCaseTransformer);
        verifyNoMoreInteractions(referredCourtDocumentTransformer);
        verifyNoMoreInteractions(listCourtHearingTransformer);
        verifyNoMoreInteractions(referredProsecutionCaseTransformer);


    }

    @Test
    public void shouldHandleCasesReferredToCourtWithDisqualificationEventMessage() throws Exception {
        // Setup
        final SjpCourtReferral sjpCourtReferral = getCourtReferral(true, false, null);
        final String caseURN = sjpCourtReferral.getProsecutionCases().get(0).getProsecutionCaseIdentifier().getCaseURN();

        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        final CourtDocument courtDocument = CourtDocument.courtDocument().build();

        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);
        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().build()));
        when(progressionService.getReferralReasonByReferralReasonId(any(), any())).thenReturn(Json.createObjectBuilder().build());
        when(progressionService.getReferralReasonByReferralReasonId(any(), any()))
                .thenReturn(Json.createObjectBuilder().add("reason", "For disqualification")
                        .build());

        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any(HearingLanguage.class), any
                (JsonEnvelope.class))).thenReturn(prosecutionCase);
        when(referredCourtDocumentTransformer.transform(any(ReferredCourtDocument.class), any
                (JsonEnvelope.class))).thenReturn(courtDocument);
        when(listCourtHearingTransformer.transform(any(), any(), any(), any(), any(UUID.class))).thenReturn
                (listCourtHearing);

        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command" +
                ".create-prosecution-case")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.create-hearing-defendant-request")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.cases-referred-to-court")
                .withUserId(randomUUID().toString()).build());

        //When
        this.eventProcessor.process(jsonEnvelope);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);
        verify(progressionService).createProsecutionCases(any(), any());
        verify(progressionService).createCourtDocument(any(), any());
        verify(summonsHearingRequestService).addDefendantRequestToHearing(eq(jsonEnvelope), any(), any(UUID.class));
        verify(referralDisqualifyWarningGenerationService).generateReferralDisqualifyWarning(eq(jsonEnvelope), any(), any(), any(ReferredDefendant.class), any());
        verify(referredProsecutionCaseTransformer).transform(any(ReferredProsecutionCase.class), eq(ENGLISH), eq(jsonEnvelope));


    }

    private SjpCourtReferral getCourtReferral(final Boolean disqualificationFlag, final Boolean hasNextHearing, final HearingLanguage hearingLanguage) {
        String referralReason = null;
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, 01, 01))
                .withReferralDate(LocalDate.of(2018, 02, 15))
                .withReferringJudicialDecision(ReferringJudicialDecision.referringJudicialDecision().withCourtHouseCode("courtHoseCode").build()).build();;

        if(disqualificationFlag) {
            referralReason = "For disqualification";
        } else {
            referralReason = "not guilty for pcnr";
        }

        final ReferredListHearingRequest listHearingRequest = ReferredListHearingRequest.referredListHearingRequest()
                .withHearingType(ReferredHearingType.referredHearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(Integer.valueOf(15))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .withHearingLanguageNeeds(hearingLanguage)
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withId(referralReasonId)
                                .withDescription(referralReason).build())
                        .build()))
                .build();

        final ReferredProsecutionCase referredProsecutionCase = ReferredProsecutionCase.referredProsecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode("CPS")
                        .withCaseURN("caseURN").build())
                .withDefendants(Arrays.asList(ReferredDefendant.referredDefendant()
                        .withId(defendantId)
                        .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                .withPersonDetails(ReferredPerson.referredPerson()
                                        .withAddress(Address.address().withPostcode("CR11111").build()).build()).build())
                        .withOffences(Arrays.asList(ReferredOffence.referredOffence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();

        final SjpCourtReferral.Builder builder = new SjpCourtReferral.Builder()
                .withSjpReferral(sjpReferral)
                .withProsecutionCases(Arrays.asList(referredProsecutionCase))
                .withListHearingRequests(Arrays.asList(listHearingRequest));

        if (hasNextHearing) {
            builder.withNextHearing(NextHearing.nextHearing().build());
        }
        return builder.build();
    }

    @Test
    public void shouldHandleSJPCasesReferredToCourtEventMessage() throws Exception {

        final SjpCourtReferral sjpCourtReferral = getCourtReferral(false, true, ENGLISH);

        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        final CourtDocument courtDocument = CourtDocument.courtDocument().build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().build()));
        when(progressionService.getReferralReasonByReferralReasonId(any(), any()))
                .thenReturn(Json.createObjectBuilder().add("reason", "reason for referral").build());
        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any(HearingLanguage.class), any
                (JsonEnvelope.class))).thenReturn(prosecutionCase);
        when(referredCourtDocumentTransformer.transform(any(ReferredCourtDocument.class), any
                (JsonEnvelope.class))).thenReturn(courtDocument);
        when(listCourtHearingTransformer.transformSjpReferralNextHearing(any(), any(), any(), any(), any())).thenReturn
                (listCourtHearing);


        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command" +
                ".create-prosecution-case")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.create-hearing-defendant-request")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        when(jsonEnvelope.metadata()).thenReturn(metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.cases-referred-to-court")
                .withUserId(randomUUID().toString()).build());

        this.eventProcessor.referSJPCasesToCourt(jsonEnvelope);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);
        verify(progressionService).createProsecutionCases(any(), any());
        verify(progressionService).createCourtDocument(any(), any());
        verify(summonsHearingRequestService).addDefendantRequestToHearing(eq(jsonEnvelope), any(), any(UUID.class));
        verify(referredProsecutionCaseTransformer).transform(any(ReferredProsecutionCase.class), eq(ENGLISH), eq(jsonEnvelope));


    }
}