package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INCOMPLETE;
import static uk.gov.moj.cpp.progression.helper.JsonHelper.createJsonEnvelope;
import static uk.gov.moj.cpp.progression.helper.JsonHelper.createMetadataWithProcessIdAndUserId;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.CrownCourtHearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Plea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.service.ListingService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.transformer.SendingSheetCompleteTransformer;


@RunWith(MockitoJUnitRunner.class)
public class ProgressionEventProcessorTest {

    private static final String CASE_ID = randomUUID().toString();
    private static final String PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED = "progression.events.sending-sheet-completed";
    private static final String GUILTY = "GUILTY";
    private static final String USER_ID = "userId";

    final ArgumentCaptor<HashMap> captor = forClass(HashMap.class);

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope messageToPublish;

    @Mock
    private ListingService listingService;

    @Spy
    @InjectMocks
    private SendingSheetCompleteTransformer sendingSheetCompleteTransformer;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();


    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private JsonObject jsonObject;

    @InjectMocks
    private ProgressionEventProcessor progressionEventProcessor;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void publishSentenceHearingAddedPublicEvent() {
        // given
        final JsonEnvelope event = createEnvelope("progression.events.sentence-hearing-date-added", createObjectBuilder().add("caseId", CASE_ID).build());


        // when
        progressionEventProcessor.publishSentenceHearingAddedPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.sentence-hearing-date-added"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishCaseAddedToCrownCourtPublicEvent() {
        // given
        final JsonEnvelope event = createEnvelope("progression.events.case-added-to-crown-court", createObjectBuilder().
                add("caseId", CASE_ID).
                add("courtCentreId", "LiverPool").
                add("status", INCOMPLETE.toString()).build());

        // when
        progressionEventProcessor.publishCaseAddedToCrownCourtPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.case-added-to-crown-court"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishCaseAlreadyExistsInCrownCourtPublicEvent() {
        // given
        final JsonEnvelope event = createEnvelope("progression.events.case-already-exists-in-crown-court", createObjectBuilder().
                add("caseId", CASE_ID).build());

        // when
        progressionEventProcessor.publishCaseAlreadyExistsInCrownCourtEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.case-already-exists-in-crown-court"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    //@Test
    public void publishSendingSheetCompletedEvent() {
        final UUID courtCenterId = randomUUID();

        // given
        final JsonEnvelope event = createJsonEnvelope(
                createMetadataWithProcessIdAndUserId(randomUUID().toString(), PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED, randomUUID().toString(), USER_ID),
                createObjectBuilder().add("hearing", createObjectBuilder()
                        .add("caseId", CASE_ID)).build());


        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(randomUUID());
        final Defendant defendant = getDefendant();
        final Offence offenceOne = new Offence();
        final UUID OFFENCE_ID = randomUUID();
        offenceOne.setId(OFFENCE_ID);
        offenceOne.setOffenceCode(CJS_OFFENCE_CODE);
        offenceOne.setPlea(new Plea(randomUUID(), GUILTY, LocalDate.now()));
        final Offence offenceTwo = new Offence();
        offenceTwo.setId(OFFENCE_ID);
        offenceTwo.setOffenceCode(CJS_OFFENCE_CODE);
        offenceTwo.setPlea(new Plea(randomUUID(), GUILTY, LocalDate.now()));
        defendant.setOffences(Arrays.asList(offenceOne, offenceTwo));
        hearing.setDefendants(Arrays.asList(defendant));
        sendingSheetCompleted.setHearing(hearing);
        final CrownCourtHearing crownCourtHearing = new CrownCourtHearing();
        crownCourtHearing.setCourtCentreId(courtCenterId);
        crownCourtHearing.setCourtCentreName("Liverpool");
        crownCourtHearing.setCcHearingDate(LocalDate.now().toString());
        sendingSheetCompleted.setCrownCourtHearing(crownCourtHearing);
        // when
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendingSheetCompleted.class)).thenReturn(sendingSheetCompleted);

        when(referenceDataOffenceService.getOffenceByCjsCode(CJS_OFFENCE_CODE, event)).thenReturn(of(getOffence("Indictable")));
        progressionEventProcessor.publishSendingSheetCompletedEvent(event);
        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor =
                forClass(JsonEnvelope.class);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.sending-sheet-completed"),
                payloadIsJson(withJsonPath(format("$.%s.%s", "hearing", "caseId"), equalTo(CASE_ID)))));

        final ArgumentCaptor<SendCaseForListing> sendCaseForListingCaptor =
                forClass(SendCaseForListing.class);

        verify(listingService).sendCaseForListing(envelopeArgumentCaptor.capture(), sendCaseForListingCaptor.capture());

        assertThat(sendCaseForListingCaptor.getValue().getHearings().get(0).getCourtCentre().getId(), is(courtCenterId));
    }

    @Test
    public void publishSendingSheetPreviouslyCompletedEvent() {
        // given
        final JsonEnvelope event = createEnvelope(
                "progression.events.sending-sheet-previously-completed",
                createObjectBuilder().add("caseId", CASE_ID).build());
        // when
        progressionEventProcessor.publishSendingSheetPreviouslyCompletedEvent(event);
        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor =
                forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(
                        "public.progression.events.sending-sheet-previously-completed"),
                payloadIsJson(withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID)))));
    }

    @Test
    public void publishSendingSheetInvalidatedEvent() {
        // given
        final JsonEnvelope event = createEnvelope(
                "progression.events.sending-sheet-invalidated",
                createObjectBuilder().add("caseId", CASE_ID).build());
        // when
        progressionEventProcessor.publishSendingSheetInvalidatedEvent(event);
        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor =
                forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(
                        "public.progression.events.sending-sheet-invalidated"),
                payloadIsJson(withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID)))));
    }

    private static JsonObject getOffence(final String modeoftrial) {
        return Json.createObjectBuilder().add("legislation", "legislation")
                .add("welshlegislation", LEGISLATION_WELSH)
                .add("title", "title")
                .add("welshoffencetitle", WELSH_OFFENCE_TITLE)
                .add("modeoftrial", modeoftrial)
                .add(CJS_OFFENCE_CODE, CJS_OFFENCE_CODE).build();

    }


    private static Defendant getDefendant() {
        final Defendant defendant = new Defendant();
        defendant.setFirstName("FN");
        defendant.setLastName("LN");
        defendant.setNationality("US");
        defendant.setGender("Male");
        defendant.setDateOfBirth(LocalDate.now().toString());
        final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Address address = new uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Address();
        address.setAddress1("line1");
        defendant.setAddress(address);
        return defendant;
    }


}
