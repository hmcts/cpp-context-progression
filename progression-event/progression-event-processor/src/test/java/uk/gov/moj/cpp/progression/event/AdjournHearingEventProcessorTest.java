package uk.gov.moj.cpp.progression.event;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingDefendant;
import uk.gov.justice.core.courts.NextHearingOffence;
import uk.gov.justice.core.courts.NextHearingProsecutionCase;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingAdjourned;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdjournHearingEventProcessorTest {

    @InjectMocks
    private AdjournHearingEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope responseEnvelope;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void listCourtHearing() throws Exception {

        final UUID previousHearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(MILLIS);
        final ZonedDateTime earliestStartDateTime = localDateTime.atZone(ZoneId.of("UTC")).truncatedTo(MILLIS);
        final Integer estimatedMinutes = 100;
        final UUID judicialId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String hearingTypeDesc = "SENTENCING";
        final String reportingReason = "Nothing";

        final UUID prosecutionCaseId1 = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        ZonedDateTime courtProceedingsInitiated = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(MILLIS);

        final UUID roomId = randomUUID();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId1)
                .withDefendants(Arrays.asList(
                        Defendant.defendant()
                                .withId(defendantId1)
                                .withMasterDefendantId(masterDefendantId)
                                .withCourtProceedingsInitiated(courtProceedingsInitiated)
                                .withOffences(
                                        Arrays.asList(
                                                Offence.offence()
                                                        .withId(offenceId1)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId3)
                                                        .build())).build(),
                        Defendant.defendant()
                                .withId(defendantId2)
                                .withOffences(Arrays.asList(
                                        Offence.offence()
                                                .withId(offenceId1)
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId3)
                                                .build()))
                                .build()))
                .build();

        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        final UUID courtApplicationId = UUID.randomUUID();
        final UUID linkedApplicationId = UUID.randomUUID();
        final UUID applicantId = UUID.randomUUID();
        final UUID applicantDefendantId = UUID.randomUUID();
        final UUID respondentId = UUID.randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withParentApplicationId(linkedApplicationId)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(applicantId)
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withMasterDefendantId(applicantDefendantId)
                                .build())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationParty.courtApplicationParty()
                        .withId(respondentId)
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(UUID.randomUUID())
                                .build())
                        .build())).build();
        final HearingAdjourned hearingAdjourned = HearingAdjourned.hearingAdjourned()
                .withAdjournedHearing(previousHearingId)
                .withNextHearings(Arrays.asList(NextHearing.nextHearing()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .withRoomId(roomId)
                                .withName("Name")
                                .withWelshName("Welsh Name")
                                .withRoomName("Room Name")
                                .withWelshRoomName("Welsh Room Name")
                                .build())
                        .withListedStartDateTime(earliestStartDateTime)
                        .withEstimatedMinutes(estimatedMinutes)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                                .withJudicialId(judicialId)
                                .withJudicialRoleType(
                                        JudicialRoleType.judicialRoleType()
                                                .withJudicialRoleTypeId(randomUUID())
                                                .withJudiciaryType("Circuit Judge")
                                                .build()
                                )
                                .build()))
                        .withType(HearingType.hearingType()
                                .withId(hearingTypeId)
                                .withDescription(hearingTypeDesc)
                                .build())
                        .withReportingRestrictionReason(reportingReason)
                        .withNextHearingProsecutionCases(Arrays.asList(NextHearingProsecutionCase.nextHearingProsecutionCase()
                                .withId(prosecutionCaseId1)
                                .withDefendants(Arrays.asList(NextHearingDefendant.nextHearingDefendant()
                                        .withId(defendantId1)
                                        .withOffences(Arrays.asList(NextHearingOffence.nextHearingOffence()
                                                .withId(offenceId1)
                                                .build()))
                                        .build()))
                                .build()))
                        .withNextHearingCourtApplicationId(Arrays.asList(courtApplication.getId()))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .build()))
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.adjourned"),
                objectToJsonObjectConverter.convert(hearingAdjourned));

        when(progressionService.getCourtApplicationByIdTyped(event, courtApplication.getId().toString())).thenReturn(Optional.of(courtApplication));
        this.eventProcessor.handleHearingAdjournedPublicEvent(event);

        final ArgumentCaptor<ListCourtHearing> listCourtHearingArgumentCaptorForListingService =
                forClass(ListCourtHearing.class);

        final ArgumentCaptor<ListCourtHearing> listCourtHearingArgumentCaptorForProgressionService =
                forClass(ListCourtHearing.class);

        verify(listingService).listCourtHearing(envelopeArgumentCaptor.capture(), listCourtHearingArgumentCaptorForListingService.capture());
        verify(progressionService).updateHearingListingStatusToSentForListing(envelopeArgumentCaptor.capture(), listCourtHearingArgumentCaptorForProgressionService.capture());

        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getId(), is(courtCentreId));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getRoomId(), is((roomId)));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getName(), is(("Name")));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getWelshName(), is(("Welsh Name")));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getRoomName(), is(("Room Name")));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getWelshRoomName(), is(("Welsh Room Name")));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getEarliestStartDateTime(), is(earliestStartDateTime));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getEstimatedMinutes(), is(estimatedMinutes));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getJurisdictionType(), is(uk.gov.justice.core.courts.JurisdictionType.CROWN));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialId(), is(judicialId));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is("Circuit Judge"));
        assertThat(listCourtHearingArgumentCaptorForListingService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is("Circuit Judge"));

        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getId(), is(courtCentreId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getRoomId(), is((roomId)));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getName(), is(("Name")));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getWelshName(), is(("Welsh Name")));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getRoomName(), is(("Room Name")));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getWelshRoomName(), is(("Welsh Room Name")));

        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getEarliestStartDateTime(), is(earliestStartDateTime));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getEstimatedMinutes(), is(estimatedMinutes));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getJurisdictionType(), is(uk.gov.justice.core.courts.JurisdictionType.CROWN));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialId(), is(judicialId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is("Circuit Judge"));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtApplications().get(0).getApplicant().getId(), is(applicantId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtApplications().get(0).getRespondents().get(0).getId(), is(respondentId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtApplications().get(0).getApplicant().getMasterDefendant().getMasterDefendantId(), is(applicantDefendantId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtApplications().get(0).getParentApplicationId(), is(linkedApplicationId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtApplications().get(0).getId(), is(courtApplicationId));
        assertThat(listCourtHearingArgumentCaptorForProgressionService.getValue().getHearings().get(0).getCourtApplicationPartyListingNeeds().get(0).getHearingLanguageNeeds().toString(), is("ENGLISH"));

    }


}
