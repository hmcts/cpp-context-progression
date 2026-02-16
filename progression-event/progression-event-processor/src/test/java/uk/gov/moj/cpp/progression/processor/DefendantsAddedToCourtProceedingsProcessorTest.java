package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings;
import static uk.gov.justice.core.courts.HearingType.hearingType;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.ListHearingRequest.listHearingRequest;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedingsV2;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateHearingWithNewDefendant;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.progression.ListingHearingRequest;
import uk.gov.moj.cpp.progression.processor.exceptions.CaseNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
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
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantsAddedToCourtProceedingsProcessorTest {

    public static final UUID PROSECUTION_CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    @InjectMocks
    private DefendantsAddedToCourtProceedingsProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private JsonObject payload;

    @Mock
    private DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings;

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
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private ListCourtHearing listCourtHearing;

    @Mock
    private SummonsHearingRequestService summonsHearingRequestService;

    final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<UUID> uuidCaptor;

    private static final UUID HEARING_ID_1 = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final UUID HEARING_ID_3 = randomUUID();

    @BeforeEach
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
        prosecutionCaseJsonObject = of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();

        final List<Hearing> futureHearings = createFutureHearings();

        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
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
        verify(sender, times(2)).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("progression.command.process-matched-defendants"));
        assertThat(envelopeCaptor.getAllValues().get(0).payload().getString("prosecutionCaseId"), is(PROSECUTION_CASE_ID.toString()));

        assertThat(envelopeCaptor.getAllValues().get(1).metadata().name(), is("public.progression.defendants-added-to-case"));
        assertThat(envelopeCaptor.getAllValues().get(1).payload(), is(payload));


        verify(listingService, times(0)).listCourtHearing(jsonEnvelope, listCourtHearing);
        verify(progressionService, times(0)).updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessageWhenProgressionCaseIsNotInViewStoreEmpty() throws Exception {
        final UUID userId = randomUUID();
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings();
        prosecutionCaseJsonObject = of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();

        final List<Hearing> futureHearings = createFutureHearings();
        //When
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("caseUrn")
                        .build())
                .build();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope,
                defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString())).thenReturn(Optional.empty());
        //Then
        assertThrows(CaseNotFoundException.class, () -> eventProcessor.process(jsonEnvelope));
    }

    public List<Hearing> createFutureHearings() {
        return singletonList(
                Hearing.hearing()
                        .withHearingDays(
                                singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
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

        final CourtCentre existingHearingCourtCentre = courtCentre().withId(randomUUID()).withRoomId(randomUUID()).build();
        final ZonedDateTime existingHearingSittingDay = ZonedDateTime.now().plusWeeks(2);

        prosecutionCaseJsonObject = of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
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


        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(), any(List.class), any(), any())).thenReturn(listCourtHearing);

        //When
        eventProcessor.process(jsonEnvelope);

        verify(listingService, times(2)).listCourtHearing(jsonEnvelope, listCourtHearing);
        verify(progressionService, times(2)).updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("progression.command.process-matched-defendants"));
        assertThat(envelopeCaptor.getAllValues().get(0).payload().getString("prosecutionCaseId"), is(PROSECUTION_CASE_ID.toString()));

        assertThat(envelopeCaptor.getAllValues().get(1).metadata().name(), is("public.progression.defendants-added-to-case"));
        assertThat(envelopeCaptor.getAllValues().get(1).payload(), is(payload));


    }

    @Test
    public void shouldHandleCasesReferredToCourtWithSameFutureHearings() throws Exception {
        final UUID userId = randomUUID();

        final CourtCentre existingHearingCourtCentre = courtCentre().withId(randomUUID()).withRoomId(randomUUID()).build();
        final ZonedDateTime existingHearingSittingDay = ZonedDateTime.now().plusWeeks(2);

        prosecutionCaseJsonObject = of(getProsecutionCaseResponse());
        final GetHearingsAtAGlance hearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(PROSECUTION_CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
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

        when(objectToJsonObjectConverter.convert(UpdateHearingWithNewDefendant.updateHearingWithNewDefendant()
                .withHearingId(HEARING_ID_1)
                .withProsecutionCaseId(PROSECUTION_CASE_ID)
                .withDefendants(defendantsAddedToCourtProceedings.getDefendants()).build()))
                .thenReturn(createObjectBuilder()
                        .add("prosecutionCaseId", PROSECUTION_CASE_ID.toString())
                        .add("hearingId", HEARING_ID_1.toString())
                        .add("offenceIds", createArrayBuilder()
                                .add(createObjectBuilder().add("offenceId", randomUUID().toString()).build())
                                .add(createObjectBuilder().add("offenceId", randomUUID().toString()).build())
                                .build())
                        .build());


        //When
        eventProcessor.process(jsonEnvelope);
        verify(sender, times(6)).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("progression.command.process-matched-defendants"));
        assertThat(envelopeCaptor.getAllValues().get(0).payload().getString("prosecutionCaseId"), is(PROSECUTION_CASE_ID.toString()));

        assertThat(envelopeCaptor.getAllValues().get(1).metadata().name(), is("public.progression.defendants-added-to-case"));
        assertThat(envelopeCaptor.getAllValues().get(1).payload(), is(payload));

        assertThat(envelopeCaptor.getAllValues().get(2).metadata().name(), is("public.progression.defendants-added-to-court-proceedings"));
        assertThat(envelopeCaptor.getAllValues().get(2).payload(), is(payload));

        assertThat(envelopeCaptor.getAllValues().get(3).metadata().name(), is("progression.command.add-or-store-defendants-and-listing-hearing-requests"));
        assertThat(envelopeCaptor.getAllValues().get(3).payload(), is(payload));

        assertThat(envelopeCaptor.getAllValues().get(4).metadata().name(), is("progression.command.update-hearing-with-new-defendant"));
        assertThat(envelopeCaptor.getAllValues().get(4).payload().getString("prosecutionCaseId"), is(PROSECUTION_CASE_ID.toString()));
        assertThat(envelopeCaptor.getAllValues().get(4).payload().getString("hearingId"), is(HEARING_ID_1.toString()));
        assertThat(envelopeCaptor.getAllValues().get(4).payload().getJsonArray("offenceIds").size(), is(2));

        verify(listingService, never()).listCourtHearing(jsonEnvelope, listCourtHearing);
        verify(progressionService, never()).updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

        //verify 1st defendant is added to hearing 1 - matching future known hearing
        verify(summonsHearingRequestService).addDefendantRequestToHearing(jsonEnvelope, getDefendantRequestFor(DEFENDANT_ID_1), HEARING_ID_1);

        //verify 2nd defendant is added to a new hearing - not matching any known hearings
        verify(summonsHearingRequestService).addDefendantRequestToHearing(eq(jsonEnvelope), eq(getDefendantRequestFor(DEFENDANT_ID_2)), uuidCaptor.capture());
        assertThat(Lists.newArrayList(HEARING_ID_1, HEARING_ID_2, HEARING_ID_3), not(hasItem(uuidCaptor.getValue())));

    }

    @Test
    public void shouldIssueDefendantsAddedToCourtProceedingsPublicEvent() {

        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.defendants-and-listing-hearing-requests-added"),
                createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(createObjectBuilder()
                                        .add("id", defendantId.toString())
                                        .add("offences", Json.createArrayBuilder().add(createObjectBuilder()
                                                        .add("id", offenceId.toString())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .add("listHearingRequests", Json.createArrayBuilder().add(createObjectBuilder()
                                        .add("listDefendantRequests", Json.createArrayBuilder().add(createObjectBuilder()
                                                        .add("defendantId", defendantId.toString())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());

        this.eventProcessor.processDefendantsAndListHearingRequestsAdded(event);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), Matchers.is("public.progression.defendants-added-to-hearing"));
        assertThat(envelopeCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.defendants[0].id", Matchers.is(defendantId.toString())))));

    }

    @Test
    public void shouldIssueDefendantsAddedToCourtProceedingsPublicEventSNI3422() {
        final String prosecutionCaseId = "9234c0ef-f000-4b41-bd90-28c8c3fe4b9b";
        final String caseURN = "AAC21170817";
        final JsonObject prosecutionCase = getSni3422Data("prosection-case-by-id.json");
        final JsonObject futureListings = getSni3422Data("listing-hearings-any-allocation.json");
        final JsonObject defendantsAddedToCourseProceedings = getSni3422Data("progression.event.defendants-added-to-court-proceedings.json");
        final JsonObjectToObjectConverter objectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.defendants-added-to-court-proceedings"),
                defendantsAddedToCourseProceedings);

        final List<Hearing> hearings = futureListings.getJsonArray("hearings").stream().map(JsonObject.class::cast).map(jsonValue -> objectConverter.convert(jsonValue, Hearing.class))
                .collect(Collectors.toList());

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), DefendantsAddedToCourtProceedings.class))
                .thenReturn(objectConverter.convert(defendantsAddedToCourseProceedings, DefendantsAddedToCourtProceedings.class));
        when(progressionService.getProsecutionCaseDetailById(eq(event), eq(prosecutionCaseId))).thenReturn(of(prosecutionCase));

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class)))
                .thenReturn(objectConverter.convert(of(prosecutionCase).get().getJsonObject("prosecutionCase"), ProsecutionCase.class));

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(GetHearingsAtAGlance.class)))
                .thenReturn(objectConverter.convert(of(prosecutionCase).get().getJsonObject("hearingsAtAGlance"), GetHearingsAtAGlance.class));
        when(listingService.getFutureHearings(any(JsonEnvelope.class), eq(caseURN)))
                .thenReturn(hearings);

        this.eventProcessor.process(event);

        verify(sender, times(6)).send(envelopeCaptor.capture());
        verify(listCourtHearingTransformer, times(0)).transform(any(JsonEnvelope.class), any(List.class), any(List.class), any(UUID.class), (Boolean) isNull());
        verify(listingService, times(0)).listCourtHearing(any(JsonEnvelope.class), any(ListCourtHearing.class));
        verify(progressionService, times(0)).updateHearingListingStatusToSentForListing(any(JsonEnvelope.class), any(ListCourtHearing.class));

        verify(summonsHearingRequestService, times(1)).addDefendantRequestToHearing(any(JsonEnvelope.class), any(List.class), any(UUID.class));

    }

    private List<ListDefendantRequest> getDefendantRequestFor(final UUID defendantId) {
        return defendantsAddedToCourtProceedings.getListHearingRequests().stream().flatMap(r -> r.getListDefendantRequests().stream().filter(dr -> dr.getDefendantId() == defendantId)).collect(Collectors.toList());
    }

    private List<Hearing> createFutureHearings(final CourtCentre existingHearingCourtCentre, final ZonedDateTime existingHearingSittingDay) {
        return Arrays.asList(
                Hearing.hearing()
                        .withHearingDays(
                                singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(existingHearingSittingDay)
                                        .build())
                        )
                        .withCourtCentreId(existingHearingCourtCentre.getId())
                        .withId(HEARING_ID_1)
                        .build(),
                Hearing.hearing()
                        .withHearingDays(
                                singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(existingHearingSittingDay)
                                        .build())
                        )
                        .withCourtCentreId(randomUUID())
                        .withId(HEARING_ID_2)
                        .build(),
                Hearing.hearing()
                        .withHearingDays(
                                singletonList(uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                                        .withStartTime(ZonedDateTime.now().plusDays(3))
                                        .build())
                        )
                        .withCourtCentreId(randomUUID())
                        .withId(HEARING_ID_3)
                        .build()
        );
    }

    private JsonObject getProsecutionCaseResponse() {
        return new StringToJsonObjectConverter().convert(getFileContents("progression.event.prosecutioncase.data.json"));
    }

    private JsonObject getSni3422Data(final String fileName) {
        String fileContents = getFileContents(fileName)
                .replaceAll("2024-08-22", LocalDate.now().toString())
                .replaceAll("2024-08-23", LocalDate.now().plusDays(2).toString());

        return new StringToJsonObjectConverter().convert(fileContents);
    }

    private String getFileContents(final String fileName) {
        try {
            return Resources.toString(getResource(fileName), defaultCharset());
        } catch (final Exception ignored) {
        }
        return StringUtils.EMPTY;
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
                .withCourtCentre(courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        final List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings()
                .withId(randomUUID())
                .withHearingDays(hearingDays2)
                .withCourtCentre(courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        hearings.add(Hearings.hearings()
                .withId(randomUUID())
                .withHearingDays(singletonList(
                        HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build()))
                .withCourtCentre(courtCentre()
                        .withId(randomUUID())
                        .withRoomId(randomUUID())
                        .build())
                .build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();
    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedings() {

        final List<Defendant> defendantsList = new ArrayList<>();

        final Offence offence = offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        final Defendant defendant = defendant()
                .withId(DEFENDANT_ID_1)
                .withProsecutionCaseId(PROSECUTION_CASE_ID)
                .withOffences(singletonList(offence))
                .build();

        defendantsList.add(defendant);

        final ListDefendantRequest listDefendantRequest = listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(singletonList(defendant.getOffences().get(0).getId()))
                .withDefendantId(defendant.getId())
                .build();

        final HearingType hearingType = hearingType().withId(randomUUID()).withDescription("TO_TRIAL").build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).build();

        final ListHearingRequest listHearingRequest = listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(singletonList(listDefendantRequest))
                .withListedStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(20)
                .build();

        return defendantsAddedToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(singletonList(listHearingRequest))
                .build();

    }

    private DefendantsAddedToCourtProceedings buildMultipleDefendantsAddedToCourtProceedings(final CourtCentre existingHearingCourtCentre, final ZonedDateTime existingHearingSittingDay) {

        final UUID prosecutionCaseId = PROSECUTION_CASE_ID;
        final List<Defendant> defendantsList = new ArrayList<>();

        final Offence offence1 = offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();
        final Defendant defendant1 = defendant()
                .withId(DEFENDANT_ID_1)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(singletonList(offence1))
                .build();

        final Offence offence2 = offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        final Defendant defendant2 = defendant()
                .withId(DEFENDANT_ID_2)
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(singletonList(offence2))
                .build();

        defendantsList.add(defendant1);
        defendantsList.add(defendant2);

        final ListDefendantRequest listDefendantRequest1 = listDefendantRequest()
                .withProsecutionCaseId(defendant1.getProsecutionCaseId())
                .withDefendantOffences(singletonList(defendant1.getOffences().get(0).getId()))
                .withDefendantId(defendant1.getId())
                .build();
        final ListDefendantRequest listDefendantRequest2 = listDefendantRequest()
                .withProsecutionCaseId(defendant2.getProsecutionCaseId())
                .withDefendantOffences(singletonList(defendant2.getOffences().get(0).getId()))
                .withDefendantId(defendant2.getId())
                .build();

        final HearingType hearingType = hearingType().withId(randomUUID()).withDescription("TO_TRIAL").build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).build();

        final ListHearingRequest listHearingRequest1 = listHearingRequest()
                .withCourtCentre(existingHearingCourtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(singletonList(listDefendantRequest1))
                .withListedStartDateTime(existingHearingSittingDay)
                .withEarliestStartDateTime(existingHearingSittingDay)
                .withEstimateMinutes(20)
                .build();
        final ListHearingRequest listHearingRequest2 = listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(singletonList(listDefendantRequest2))
                .withListedStartDateTime(ZonedDateTime.now().plusWeeks(3))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withEstimateMinutes(20)
                .build();

        return defendantsAddedToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Arrays.asList(listHearingRequest1, listHearingRequest2))
                .build();

    }

    private MetadataBuilder getMetadataBuilder(final UUID userId, final String name) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(name)
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId((Objects.isNull(userId) ? randomUUID() : userId).toString());
    }

    @Test
    void shouldHandleDefendantsAddedToCourtProceedingsV2Event() throws Exception {
        final UUID userId = randomUUID();
        final DefendantsAddedToCourtProceedingsV2 defendantsAddedToCourtProceedingsV2 = buildDefendantsAddedToCourtProceedingsV2();
        prosecutionCaseJsonObject = of(getProsecutionCaseResponse());
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("caseUrn")
                        .build())
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(userId, "progression.event.defendants-added-to-court-proceedings-v2").build());
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedingsV2.class))
                .thenReturn(defendantsAddedToCourtProceedingsV2);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class),
                eq(defendantsAddedToCourtProceedingsV2.getDefendants().get(0).getProsecutionCaseId().toString()))).thenReturn(prosecutionCaseJsonObject);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                ProsecutionCase.class)).thenReturn(prosecutionCase);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildDefendantsAddedToCourtProceedings(defendantsAddedToCourtProceedingsV2));

        eventProcessor.processV2(jsonEnvelope);
        
        verify(sender, times(6)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("progression.command.process-matched-defendants"));
        assertThat(envelopeCaptor.getAllValues().get(1).metadata().name(), is("public.progression.defendants-added-to-case"));
    }

    @Test
    public void shouldHandleDefendantsAddedToCourtProceedingsV2EventWhenCaseNotFound() throws Exception {
        final UUID userId = randomUUID();
        final DefendantsAddedToCourtProceedingsV2 defendantsAddedToCourtProceedingsV2 = buildDefendantsAddedToCourtProceedingsV2();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(userId, "progression.event.defendants-added-to-court-proceedings-v2").build());
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedingsV2.class))
                .thenReturn(defendantsAddedToCourtProceedingsV2);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class),
                eq(defendantsAddedToCourtProceedingsV2.getDefendants().get(0).getProsecutionCaseId().toString()))).thenReturn(Optional.empty());
        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildDefendantsAddedToCourtProceedings(defendantsAddedToCourtProceedingsV2));

        assertThrows(CaseNotFoundException.class, () -> eventProcessor.processV2(jsonEnvelope));
    }

    @Test
    public void shouldHandleDefendantsAddedToCourtProceedingsV2WithHearingInitialisedStatus() throws Exception {
        final UUID userId = randomUUID();
        final DefendantsAddedToCourtProceedingsV2 defendantsAddedToCourtProceedingsV2 = buildDefendantsAddedToCourtProceedingsV2WithHearingInitialised();
        prosecutionCaseJsonObject = of(getProsecutionCaseResponse());
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("caseUrn")
                        .build())
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(userId, "progression.event.defendants-added-to-court-proceedings-v2").build());
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedingsV2.class))
                .thenReturn(defendantsAddedToCourtProceedingsV2);
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class),
                eq(defendantsAddedToCourtProceedingsV2.getDefendants().get(0).getProsecutionCaseId().toString()))).thenReturn(prosecutionCaseJsonObject);
        when(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(), any(List.class), any(), any())).thenReturn(listCourtHearing);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildDefendantsAddedToCourtProceedings(defendantsAddedToCourtProceedingsV2));


        eventProcessor.processV2(jsonEnvelope);
        
        verify(listingService).listCourtHearing(any(JsonEnvelope.class), eq(listCourtHearing));
        verify(progressionService).updateHearingListingStatusToSentForListing(any(JsonEnvelope.class), eq(listCourtHearing));
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(JsonEnvelope.class), any(List.class), any(UUID.class));
    }

    private DefendantsAddedToCourtProceedingsV2 buildDefendantsAddedToCourtProceedingsV2() {
        final List<Defendant> defendantsList = new ArrayList<>();
        final Offence offence = offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        final Defendant defendant = defendant()
                .withId(DEFENDANT_ID_1)
                .withProsecutionCaseId(PROSECUTION_CASE_ID)
                .withOffences(singletonList(offence))
                .build();

        defendantsList.add(defendant);

        final HearingType hearingType = hearingType().withId(randomUUID()).withDescription("TO_TRIAL").build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).build();

        final ListDefendantRequest listDefendantRequest = listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(singletonList(defendant.getOffences().get(0).getId()))
                .withDefendantId(defendant.getId())
                .build();

        final ListHearingRequest listHearingRequest = listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(singletonList(listDefendantRequest))
                .withListedStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(20)
                .build();

        final uk.gov.moj.cpp.progression.ListingHearingRequest listingHearingRequest = uk.gov.moj.cpp.progression.ListingHearingRequest.listingHearingRequest()
                .withHearingId(randomUUID())
                .withListHearingRequest(listHearingRequest)
                .withHearingListingStatus(uk.gov.justice.core.courts.HearingListingStatus.SENT_FOR_LISTING)
                .build();

        return DefendantsAddedToCourtProceedingsV2.defendantsAddedToCourtProceedingsV2()
                .withDefendants(defendantsList)
                .withListingHearingRequests(singletonList(listingHearingRequest))
                .build();
    }

    private JsonObject buildDefendantsAddedToCourtProceedings(DefendantsAddedToCourtProceedingsV2 v2payload) {

        final DefendantsAddedToCourtProceedings addedToCourtProceedings = defendantsAddedToCourtProceedings()
                .withDefendants(v2payload.getDefendants())
                .withListHearingRequests(v2payload.getListingHearingRequests().stream().map(ListingHearingRequest::getListHearingRequest).toList())
                .build();

        return new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()).convert(addedToCourtProceedings);
    }

    private DefendantsAddedToCourtProceedingsV2 buildDefendantsAddedToCourtProceedingsV2WithHearingInitialised() {
        final List<Defendant> defendantsList = new ArrayList<>();
        final Offence offence = offence()
                .withId(randomUUID())
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.now().minusWeeks(1))
                .withCount(0)
                .build();

        final Defendant defendant = defendant()
                .withId(DEFENDANT_ID_1)
                .withProsecutionCaseId(PROSECUTION_CASE_ID)
                .withOffences(singletonList(offence))
                .build();

        defendantsList.add(defendant);

        final HearingType hearingType = hearingType().withId(randomUUID()).withDescription("TO_TRIAL").build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).build();

        final ListDefendantRequest listDefendantRequest = listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(singletonList(defendant.getOffences().get(0).getId()))
                .withDefendantId(defendant.getId())
                .build();

        final ListHearingRequest listHearingRequest = listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(singletonList(listDefendantRequest))
                .withListedStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(20)
                .build();

        final uk.gov.moj.cpp.progression.ListingHearingRequest listingHearingRequest = uk.gov.moj.cpp.progression.ListingHearingRequest.listingHearingRequest()
                .withHearingId(randomUUID())
                .withListHearingRequest(listHearingRequest)
                .withHearingListingStatus(uk.gov.justice.core.courts.HearingListingStatus.HEARING_INITIALISED)
                .build();

        return DefendantsAddedToCourtProceedingsV2.defendantsAddedToCourtProceedingsV2()
                .withDefendants(defendantsList)
                .withListingHearingRequests(singletonList(listingHearingRequest))
                .build();
    }

}
