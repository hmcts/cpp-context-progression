package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantsAddedToCourtProceedingsProcessorTest {

    @InjectMocks
    private DefendantsAddedToCourtProceedingsProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private JsonObject payload;

    @Mock
    private DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings;

    @InjectMocks
    private Optional<JsonObject> prosecutionCaseJsonObject;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private ListCourtHearing listCourtHearing;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() throws Exception {
        final UUID userId = randomUUID();
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(userId, "progression.event.defendants-added-to-court-proceedings").build());

        defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings();
        prosecutionCaseJsonObject = Optional.of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();

        final List<Hearing> futureHearings = createFutureHearings();

        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("caseUrn")
                        .build())
                .build();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope,
                defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString())).thenReturn(prosecutionCaseJsonObject);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("hearingsAtAGlance"),
                GetHearingsAtAGlance.class)).thenReturn(hearingsAtAGlance);

        when(listingService.getFutureHearings(jsonEnvelope, "caseUrn")).thenReturn(futureHearings);

        //When
        eventProcessor.process(jsonEnvelope);
        verify(sender, times(1)).send(any());
    }

    public List<Hearing> createFutureHearings() {
        return Collections.singletonList(
                Hearing.hearing()
                        .withHearingDays(
                                Collections.singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(ZonedDateTime.now().plusDays(3))
                                        .build())
                        ).build()
        );
    }

    public List<Hearing> createNoFutureHearings() {
        return Collections.emptyList();
    }

    @Test
    public void shouldHandleCasesReferredToCourtWithNoFutureHearings() throws Exception {
        final UUID userId = randomUUID();
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(userId, "progression.event.defendants-added-to-court-proceedings").build());

        final CourtCentre existingHearingCourtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(randomUUID()).build();
        final ZonedDateTime existingHearingSittingDay = ZonedDateTime.now().plusWeeks(2);

        prosecutionCaseJsonObject = Optional.of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("caseUrn")
                        .build())
                .build();


        defendantsAddedToCourtProceedings = buildMultipleDefendantsAddedToCourtProceedings(existingHearingCourtCentre, existingHearingSittingDay);

        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope,
                defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString())).thenReturn(prosecutionCaseJsonObject);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("hearingsAtAGlance"),
                GetHearingsAtAGlance.class)).thenReturn(hearingsAtAGlance);


        final List<Hearing> futureHearings = createNoFutureHearings();

        when(listingService.getFutureHearings(jsonEnvelope, "caseUrn")).thenReturn(futureHearings);


        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(), any())).thenReturn(listCourtHearing);

        //When
        eventProcessor.process(jsonEnvelope);

        verify(listingService,times(1)).listCourtHearing(jsonEnvelope, listCourtHearing);

        verify(sender, times(1)).send(any());

    }

    @Test
    public void shouldHandleCasesReferredToCourtWithSameFutureHearings() throws Exception {
        final UUID userId = randomUUID();

        final CourtCentre existingHearingCourtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(randomUUID()).build();
        final ZonedDateTime existingHearingSittingDay = ZonedDateTime.now().plusWeeks(2);

        prosecutionCaseJsonObject = Optional.of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("caseUrn")
                        .build())
                .build();

        defendantsAddedToCourtProceedings = buildMultipleDefendantsAddedToCourtProceedings(existingHearingCourtCentre, existingHearingSittingDay);

        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(userId, "progression.event.defendants-added-to-court-proceedings").build());


        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope,
                defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString())).thenReturn(prosecutionCaseJsonObject);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("hearingsAtAGlance"),
                GetHearingsAtAGlance.class)).thenReturn(hearingsAtAGlance);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                ProsecutionCase.class)).thenReturn(prosecutionCase);

        final List<Hearing> futureHearings = createFutureHearings(existingHearingCourtCentre, existingHearingSittingDay);

        when(listingService.getFutureHearings(jsonEnvelope, "caseUrn")).thenReturn(futureHearings);


        //When
        eventProcessor.process(jsonEnvelope);
        verify(sender, times(2)).send(any());
        verify(listingService,times(0)).listCourtHearing(jsonEnvelope, listCourtHearing);

    }

    private List<Hearing> createFutureHearings(final CourtCentre existingHearingCourtCentre, final ZonedDateTime existingHearingSittingDay) {
        return Arrays.asList(
                Hearing.hearing()
                        .withHearingDays(
                                Collections.singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(existingHearingSittingDay)
                                        .build())
                        )
                        .withCourtCentreId(existingHearingCourtCentre.getId())
                        .build(),
                Hearing.hearing()
                        .withHearingDays(
                                Collections.singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(existingHearingSittingDay)
                                        .build())
                        )
                        .withCourtCentreId(randomUUID())
                        .build(),
                Hearing.hearing()
                        .withHearingDays(
                                Collections.singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(ZonedDateTime.now().plusDays(3))
                                        .build())
                        )
                        .withCourtCentreId(randomUUID())
                        .build()
        );
    }

    private JsonObject getProsecutionCaseResponse() {
        String response = null;
        try {
            response = Resources.toString(getResource("progression.event.prosecutioncase.data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }

    private GetHearingsAtAGlance getCaseAtAGlanceWithFutureHearings() throws Exception {

        final List<Hearings> hearings = new ArrayList<>();

        final List<HearingDay> hearingDays = new ArrayList<>();

        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings()
                .withId(randomUUID())
                .withHearingDays(hearingDays)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        final List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings()
                .withId(UUID.randomUUID())
                .withHearingDays(hearingDays2)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        hearings.add(Hearings.hearings()
                .withId(UUID.randomUUID())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();

    }

    private GetHearingsAtAGlance getCaseAtAGlanceWithFutureHearings(final CourtCentre existingHearingCourtCentre, final ZonedDateTime existingHearingSittingDay) throws Exception {

        final List<Hearings> hearings = new ArrayList<>();

        final List<HearingDay> hearingDays = new ArrayList<>();

        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(existingHearingSittingDay).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings()
                .withId(randomUUID())
                .withHearingDays(hearingDays)
                .withCourtCentre(existingHearingCourtCentre)
                .build());

        final List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings()
                .withId(UUID.randomUUID())
                .withHearingDays(hearingDays2)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        hearings.add(Hearings.hearings()
                .withId(UUID.randomUUID())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();

    }


    private GetHearingsAtAGlance getCaseAtAGlanceWithNoFutureHearings() throws Exception {

        final List<Hearings> hearings = new ArrayList<>();

        final List<HearingDay> hearingDays = new ArrayList<>();

        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(5)).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays).build());

        final List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(4)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays2).build());

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(Collections.singletonList(
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusWeeks(1)).build())).build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();

    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedings() {

        final List<Defendant> defendantsList = new ArrayList<>();

        final Offence offence = Offence.offence()
                .withId(UUID.randomUUID())
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withProsecutionCaseId(UUID.randomUUID())
                .withOffences(Collections.singletonList(offence))
                .build();

        defendantsList.add(defendant);

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(Collections.singletonList(defendant.getOffences().get(0).getId()))
                .withDefendantId(defendant.getId())
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_TRIAL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest))
                .withListedStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(new Integer(20))
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

    }

    private DefendantsAddedToCourtProceedings buildMultipleDefendantsAddedToCourtProceedings(final CourtCentre existingHearingCourtCentre, final ZonedDateTime existingHearingSittingDay) {

        final UUID prosecutionCaseId = randomUUID();
        final List<Defendant> defendantsList = new ArrayList<>();

        final Offence offence1 = Offence.offence()
                .withId(UUID.randomUUID())
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();
        final Defendant defendant1 = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(Collections.singletonList(offence1))
                .build();

        final Offence offence2 = Offence.offence()
                .withId(UUID.randomUUID())
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        final Defendant defendant2 = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(Collections.singletonList(offence2))
                .build();

        defendantsList.add(defendant1);
        defendantsList.add(defendant2);

        final ListDefendantRequest listDefendantRequest1 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant1.getProsecutionCaseId())
                .withDefendantOffences(Collections.singletonList(defendant1.getOffences().get(0).getId()))
                .withDefendantId(defendant1.getId())
                .build();
        final ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant2.getProsecutionCaseId())
                .withDefendantOffences(Collections.singletonList(defendant2.getOffences().get(0).getId()))
                .withDefendantId(defendant2.getId())
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_TRIAL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest1 = ListHearingRequest.listHearingRequest()
                .withCourtCentre(existingHearingCourtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest1))
                .withListedStartDateTime(existingHearingSittingDay)
                .withEarliestStartDateTime(existingHearingSittingDay)
                .withEstimateMinutes(new Integer(20))
                .build();
        final ListHearingRequest listHearingRequest2 = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest2))
                .withListedStartDateTime(ZonedDateTime.now().plusWeeks(3))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withEstimateMinutes(new Integer(20))
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Arrays.asList(listHearingRequest1, listHearingRequest2))
                .build();

    }

    private MetadataBuilder getMetadataBuilder(final UUID userId, final String name) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(name)
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId((Objects.isNull(userId) ? randomUUID() : userId).toString());
    }

}
