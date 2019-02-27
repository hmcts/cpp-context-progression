package uk.gov.moj.cpp.progression.event;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingDefendant;
import uk.gov.justice.core.courts.NextHearingOffence;
import uk.gov.justice.core.courts.NextHearingProsecutionCase;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.hearing.courts.HearingAdjourned;
import uk.gov.justice.hearing.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
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
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void sendCaseForListing() throws Exception {

        final UUID previousHearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDateTime localDateTime = LocalDateTime.now();
        final ZonedDateTime earliestStartDateTime = localDateTime.atZone(ZoneId.of("UTC"));
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

        final UUID roomId = randomUUID();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId1)
                .withDefendants(Arrays.asList(
                        Defendant.defendant()
                                .withId(defendantId1)
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
                        .withEarliestStartDateTime(earliestStartDateTime)
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
                        .build()))
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.adjourned"),
                objectToJsonObjectConverter.convert(hearingAdjourned));

        when(progressionService.getProsecutionCaseDetailById(event, prosecutionCaseId1.toString())).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));
        this.eventProcessor.handleHearingAdjournedPublicEvent(event);

        final ArgumentCaptor<SendCaseForListing> sendCaseForListingCaptorForListingService =
                forClass(SendCaseForListing.class);

        final ArgumentCaptor<SendCaseForListing> sendCaseForListingCaptorForProgressionService =
                forClass(SendCaseForListing.class);

        verify(listingService).sendCaseForListing(envelopeArgumentCaptor.capture(), sendCaseForListingCaptorForListingService.capture());
        verify(progressionService).updateHearingListingStatusToSentForListing(envelopeArgumentCaptor.capture(), sendCaseForListingCaptorForProgressionService.capture());

        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getId(), is(courtCentreId));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getRoomId(), is((roomId)));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getName(), is(("Name")));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getWelshName(), is(("Welsh Name")));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getRoomName(), is(("Room Name")));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getCourtCentre().getWelshRoomName(), is(("Welsh Room Name")));

        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getEarliestStartDateTime(), is(earliestStartDateTime));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getEstimatedMinutes(), is(estimatedMinutes));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getJurisdictionType(), is(uk.gov.justice.core.courts.JurisdictionType.CROWN));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialId(), is(judicialId));
        assertThat(sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is("Circuit Judge"));

        List<ProsecutionCase> responseProsecutionCases = sendCaseForListingCaptorForListingService.getValue().getHearings().get(0).getProsecutionCases();
        assertThat(responseProsecutionCases.size(), is(1));
        assertThat(responseProsecutionCases.get(0).getId(), is(prosecutionCaseId1));
        assertThat(responseProsecutionCases.get(0).getDefendants().size(), is(1));
        assertThat(responseProsecutionCases.get(0).getDefendants().get(0).getId(), is(defendantId1));
        assertThat(responseProsecutionCases.get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(responseProsecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));

        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getId(), is(courtCentreId));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getRoomId(), is((roomId)));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getName(), is(("Name")));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getWelshName(), is(("Welsh Name")));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getRoomName(), is(("Room Name")));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getCourtCentre().getWelshRoomName(), is(("Welsh Room Name")));

        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getEarliestStartDateTime(), is(earliestStartDateTime));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getEstimatedMinutes(), is(estimatedMinutes));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getJurisdictionType(), is(uk.gov.justice.core.courts.JurisdictionType.CROWN));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialId(), is(judicialId));
        assertThat(sendCaseForListingCaptorForProgressionService.getValue().getHearings().get(0).getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is("Circuit Judge"));
    }


}
