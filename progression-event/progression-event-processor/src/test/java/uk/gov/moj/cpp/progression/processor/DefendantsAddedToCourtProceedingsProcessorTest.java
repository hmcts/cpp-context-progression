package uk.gov.moj.cpp.progression.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import com.google.common.io.Resources;
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
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

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
    private GetCaseAtAGlance caseAtAGlance;

    @Spy
    private final Enveloper enveloper = createEnveloper();
    private final String PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";

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

    private final String prosecutionCaseSampleDataPath = "progression.event.prosecutioncase.data.json";

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() throws Exception {
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings();

        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);

        caseAtAGlance = getCaseAtAGlanceWithFutureHearings();

        prosecutionCaseJsonObject = Optional.of(getProcecutionCaseResponse());

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope,
                defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString())).thenReturn(prosecutionCaseJsonObject);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("caseAtAGlance"),
                GetCaseAtAGlance.class)).thenReturn(caseAtAGlance);

        when(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        eventProcessor.process(jsonEnvelope);
        verify(sender).send(finalEnvelope);

        //Given
        caseAtAGlance = getCaseAtAGlanceWithNoFutureHearings();

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("caseAtAGlance"),
                GetCaseAtAGlance.class)).thenReturn(caseAtAGlance);

        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(),any(), any())).thenReturn(listCourtHearing);

        //When
        eventProcessor.process(jsonEnvelope);
        verify(listingService).listCourtHearing(jsonEnvelope, listCourtHearing);

    }

    private JsonObject getProcecutionCaseResponse() {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(prosecutionCaseSampleDataPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
        }

        return new StringToJsonObjectConverter().convert(response);
    }

    private GetCaseAtAGlance getCaseAtAGlanceWithFutureHearings() throws Exception {

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();

        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays2).build());

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(Collections.singletonList(
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build())).build());

        return GetCaseAtAGlance.getCaseAtAGlance().withHearings(hearings).build();

    }

    private GetCaseAtAGlance getCaseAtAGlanceWithNoFutureHearings() throws Exception {

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();

        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(5)).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(4)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays2).build());

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(Collections.singletonList(
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusWeeks(1)).build())).build());

        return GetCaseAtAGlance.getCaseAtAGlance().withHearings(hearings).build();

    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedings() {

        final List<Defendant> defendantsList = new ArrayList<>();

        Offence offence = Offence.offence()
                .withId(UUID.randomUUID())
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withProsecutionCaseId(UUID.randomUUID())
                .withOffences(Collections.singletonList(offence))
                .build();

        defendantsList.add(defendant);

        ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(Collections.singletonList(defendant.getOffences().get(0).getId()))
                .withDefendantId(defendant.getId())
                .build();

        HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_TRIAL").build();
        CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(new Integer(20))
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

    }

}
