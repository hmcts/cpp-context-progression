package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.json.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.listing.courts.Defendants;
import uk.gov.justice.listing.courts.HearingPartiallyUpdated;
import uk.gov.justice.listing.courts.Offences;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

@RunWith(MockitoJUnitRunner.class)
public class ListHearingRequestedProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID MULTI_OFFENCE_DEFENDANT_ID = randomUUID();
    private static final UUID SAME_DEFENDANT_OFFENCE_ID_1 = randomUUID();
    private static final UUID SAME_DEFENDANT_OFFENCE_ID_2 = randomUUID();

    static final List<Offence> offences = new ArrayList<Offence>() {{
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_1).build());
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_2).build());
    }};

    private static final uk.gov.justice.core.courts.Defendant multiOffenceDefendant = uk.gov.justice.core.courts.Defendant.defendant().withId(MULTI_OFFENCE_DEFENDANT_ID)
            .withOffences(offences)
            .withProsecutionCaseId(CASE_ID)
            .withPersonDefendant(PersonDefendant.personDefendant().build()).build();

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);


    @Mock
    private Sender sender;

    @InjectMocks
    private ListHearingRequestedProcessor listHearingRequestedProcessor;

    @Captor
    private ArgumentCaptor<ListCourtHearing> listingCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> senderCaptor;

    @Test
    public void shouldCallCommands(){
        final ListHearingRequested listHearingRequested = ListHearingRequested.listHearingRequested()
                .withHearingId(UUID.randomUUID())
                .withListNewHearing(receivePayloadOfListHearingRequestWithOneCaseMultipleDefendantsWithReferralReason())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(listHearingRequested);


        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                payload);

        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));
        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(List.class), any(CourtHearingRequest.class), any(UUID.class))).thenReturn(listCourtHearing);

        listHearingRequestedProcessor.handle(requestMessage);

        verify(listingService).listCourtHearing(any(JsonEnvelope.class), listingCaptor.capture());
        assertThat(listingCaptor.getValue(), is(listCourtHearing));

        verify(progressionService).updateHearingListingStatusToSentForListing(any(JsonEnvelope.class), listingCaptor.capture());
        assertThat(listingCaptor.getValue(), is(listCourtHearing));

    }

    @Test
    public void souldCallCommand() {

        final HearingPartiallyUpdated hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(randomUUID())
                .withProsecutionCases(singletonList(ProsecutionCases.prosecutionCases()
                        .withCaseId(randomUUID())
                        .withDefendants(singletonList(Defendants.defendants()
                                .withDefendantId(randomUUID())
                                .withOffences(singletonList(Offences.offences()
                                        .withOffenceId(randomUUID())
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingPartiallyUpdated);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-partially-updated"),
                payload);

        listHearingRequestedProcessor.handlePublicEventForPartiallyUpdate(requestMessage);

        verify(sender).send(senderCaptor.capture());
        assertThat(senderCaptor.getValue().metadata().name(), is("progression.command.update-hearing-for-partial-allocation"));

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = jsonObjectToObjectConverter.convert(senderCaptor.getValue().payload(), UpdateHearingForPartialAllocation.class);

        assertThat(updateHearingForPartialAllocation.getHearingId(), is(hearingPartiallyUpdated.getHearingIdToBeUpdated()));
        assertThat(updateHearingForPartialAllocation.getProsecutionCasesToRemove().get(0).getCaseId(), is(hearingPartiallyUpdated.getProsecutionCases().get(0).getCaseId()));
        assertThat(updateHearingForPartialAllocation.getProsecutionCasesToRemove().get(0).getDefendantsToRemove().get(0).getDefendantId(), is(hearingPartiallyUpdated.getProsecutionCases().get(0).getDefendants().get(0).getDefendantId()));
        assertThat(updateHearingForPartialAllocation.getProsecutionCasesToRemove().get(0).getDefendantsToRemove().get(0).getOffencesToRemove().get(0).getOffenceId(), is(hearingPartiallyUpdated.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId()));

    }

    @Test
    public void shouldEnrichCourtCenterWhenCallNewHearng(){

        final HearingRequestedForListing hearingRequestedForListing = HearingRequestedForListing.hearingRequestedForListing()
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .build())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-requested-for-listing"),
                objectToJsonObjectConverter.convert(hearingRequestedForListing));

        final CourtCentre enrichedCourtCenter = CourtCentre.courtCentre().build();
        when(progressionService.transformCourtCentre(any(), any())).thenReturn(enrichedCourtCenter);
        listHearingRequestedProcessor.handlePublicEvent(requestMessage);

        verify(sender).send(senderCaptor.capture());
        assertThat(senderCaptor.getValue().metadata().name(), is("progression.command.list-new-hearing"));

        final HearingRequestedForListing command = jsonObjectToObjectConverter.convert(senderCaptor.getValue().payload(), HearingRequestedForListing.class);
        assertThat(command.getListNewHearing().getCourtCentre(), is(enrichedCourtCenter));

    }

    private CourtHearingRequest receivePayloadOfListHearingRequestWithOneCaseMultipleDefendantsWithReferralReason(){
        return CourtHearingRequest.courtHearingRequest()
                        .withHearingType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("typeDescription")
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(randomUUID())
                                .withName("Court 1")
                                .build())
                        .withJurisdictionType(JurisdictionType.MAGISTRATES)
                        .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                                .withProsecutionCaseId(CASE_ID)
                                .withDefendantOffences(asList(randomUUID(), randomUUID()))
                                .withDefendantId(MULTI_OFFENCE_DEFENDANT_ID)
                                .build(), ListDefendantRequest.listDefendantRequest()
                                .withProsecutionCaseId(CASE_ID)
                                .withDefendantOffences(asList(randomUUID(), randomUUID()))
                                .withDefendantId(randomUUID())
                                .build()))
                        .build();
    }

    private ProsecutionCase getProsecutionCaseWithMultiOffence() {
        return ProsecutionCase.prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(CASE_ID)
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(Arrays.asList(multiOffenceDefendant))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
    }
}
