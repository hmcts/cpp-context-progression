package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Plea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.helper.JsonHelper;
import uk.gov.moj.cpp.progression.activiti.workflow.listhearing.converter.SendingSheetCompleteToListingCaseConverter;
import uk.gov.moj.cpp.progression.activiti.workflow.listhearing.ListHearingService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.json.Json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionEventProcessorTest {

    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final String PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED = "progression.events.sending-sheet-completed";
    private static final String GUILTY = "GUILTY";
    private static final String CASE_ID_FIELD = "caseId";
    private static final String SEND_CASE_FORLISTING_PAYLOAD = "sendCaseForlistingPayload";
    private static final String USER_ID = "userId";
    private static final String LISTING = "LISTING";
    private static final String HEARING_ID = "hearingId";
    private static final String WHEN = "WHEN";
    private static final String PLEA = "PLEA";

    final ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope messageToPublish;

    @Mock
    private ListHearingService listHearingService;

    @Mock
    private SendingSheetCompleteToListingCaseConverter sendingSheetCompleteToListingCaseConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(this.objectMapper);

    @InjectMocks
    private ProgressionEventProcessor progressionEventProcessor;

    @Test
    public void publishSentenceHearingAddedPublicEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.sentence-hearing-date-added", createObjectBuilder().add("caseId", CASE_ID).build());


        // when
        this.progressionEventProcessor.publishSentenceHearingAddedPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(this.sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.sentence-hearing-date-added"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishCaseAddedToCrownCourtPublicEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.case-added-to-crown-court", createObjectBuilder().
                add("caseId", CASE_ID).
                add("courtCentreId","LiverPool").
                add("status", CaseStatusEnum.INCOMPLETE.toString()).build());

        // when
        this.progressionEventProcessor.publishCaseAddedToCrownCourtPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(this.sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.case-added-to-crown-court"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishCaseAlreadyExistsInCrownCourtPublicEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.case-already-exists-in-crown-court", createObjectBuilder().
                add("caseId", CASE_ID).build());

        // when
        this.progressionEventProcessor.publishCaseAlreadyExistsInCrownCourtEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(this.sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.case-already-exists-in-crown-court"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishSendingSheetCompletedEvent() {

        // given
        final JsonEnvelope event = JsonHelper.createJsonEnvelope(
                JsonHelper.createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED, UUID.randomUUID().toString(), ProcessMapConstant.USER_ID),
                createObjectBuilder().add("hearing", createObjectBuilder()
                        .add("caseId", CASE_ID)).build());


        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.randomUUID());
        final Defendant defendant = new Defendant();
        final Offence offenceOne = new Offence();
        offenceOne.setPlea(new Plea(UUID.randomUUID(), GUILTY, null));
        final Offence offenceTwo = new Offence();
        offenceTwo.setPlea(new Plea(UUID.randomUUID(), GUILTY, null));
        defendant.setOffences(Arrays.asList(offenceOne, offenceTwo));
        hearing.setDefendants(Arrays.asList(defendant));
        sendingSheetCompleted.setHearing(hearing);
        final ListingCase listingCase = new ListingCase(UUID.fromString(CASE_ID), null, null);
        // when

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendingSheetCompleted.class)).thenReturn(sendingSheetCompleted);
        when(sendingSheetCompleteToListingCaseConverter.convert(sendingSheetCompleted)).thenReturn(listingCase);

        this.progressionEventProcessor.publishSendingSheetCompletedEvent(event);
        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor =
                        ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(this.sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName("public.progression.events.sending-sheet-completed"),
                        payloadIsJson(withJsonPath(format("$.%s.%s", "hearing","caseId"), equalTo(CASE_ID)))));
    }

    @Test
    public void publishSendingSheetPreviouslyCompletedEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope(
                        "progression.events.sending-sheet-previously-completed",
                        createObjectBuilder().add("caseId", CASE_ID).build());
        // when
        this.progressionEventProcessor.publishSendingSheetPreviouslyCompletedEvent(event);
        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor =
                        ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(this.sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                        metadata().withName(
                                        "public.progression.events.sending-sheet-previously-completed"),
                        payloadIsJson(withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID)))));
    }

    @Test
    public void publishSendingSheetInvalidatedEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope(
                "progression.events.sending-sheet-invalidated",
                createObjectBuilder().add("caseId", CASE_ID).build());
        // when
        this.progressionEventProcessor.publishSendingSheetInvalidatedEvent(event);
        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(this.sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(
                        "public.progression.events.sending-sheet-invalidated"),
                payloadIsJson(withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID)))));
    }


    @Test
    public void shouldProcessEventRaiseSendCaseForListing_when_Defendant_Guilty() throws Exception {
        //given
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.randomUUID());
        final Defendant defendant = new Defendant();
        final Offence offenceOne = new Offence();
        offenceOne.setPlea( new Plea(UUID.randomUUID(),GUILTY,null));
        final Offence offenceTwo = new Offence();
        offenceTwo.setPlea(new Plea(UUID.randomUUID(),GUILTY,null));
        defendant.setOffences(Arrays.asList(offenceOne, offenceTwo));
        hearing.setDefendants(Arrays.asList(defendant));
        sendingSheetCompleted.setHearing(hearing);
        final JsonEnvelope jsonEnvelope = JsonHelper.createJsonEnvelope(
                JsonHelper.createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED, UUID.randomUUID().toString(), ProcessMapConstant.USER_ID),
                Json.createObjectBuilder().add("hearing", createObjectBuilder().add("caseId", sendingSheetCompleted.getHearing().getCaseId().toString())).build());
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SendingSheetCompleted.class)).thenReturn(sendingSheetCompleted);
        final ListingCase listingCase = new ListingCase(hearing.getCaseId(), null, null);
        when(sendingSheetCompleteToListingCaseConverter.convert(sendingSheetCompleted)).thenReturn(listingCase);

        //when
        progressionEventProcessor.publishSendingSheetCompletedEvent(jsonEnvelope);

        //then
        verify(listHearingService).startProcess(captor.capture());

        Assert.assertThat(captor.getValue().get(CASE_ID_FIELD), IsEqual.equalTo(listingCase.getCaseId()));
        Assert.assertThat((ListingCase) captor.getValue().get(SEND_CASE_FORLISTING_PAYLOAD), IsEqual.equalTo(listingCase));
        Assert.assertThat(captor.getValue().get(USER_ID), IsEqual.equalTo(ProcessMapConstant.USER_ID));
        Assert.assertThat(UUID.fromString(captor.getValue().get(HEARING_ID).toString()).toString().length(), IsEqual.equalTo(36));
        Assert.assertThat(captor.getValue().get(WHEN), IsEqual.equalTo("Sending Sheet Complete"));
        Assert.assertThat(((List<String>) captor.getValue().get(PLEA)).contains(GUILTY), IsEqual.equalTo(true));
    }

    @Test
    public void shouldProcessEventRaiseSendCaseForListing_when_Defendant_Not_Guilty() throws Exception {
        //given
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.randomUUID());
        final Defendant defendant = new Defendant();
        final Offence offenceOne = new Offence();
        offenceOne.setPlea(new Plea(UUID.randomUUID(),"NOT GUILTY",null));
        final Offence offenceTwo = new Offence();
        defendant.setOffences(Arrays.asList(offenceOne, offenceTwo));
        hearing.setDefendants(Arrays.asList(defendant));
        sendingSheetCompleted.setHearing(hearing);
        final JsonEnvelope jsonEnvelope = JsonHelper.createJsonEnvelope(
                JsonHelper.createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED, UUID.randomUUID().toString(), ProcessMapConstant.USER_ID),
                Json.createObjectBuilder().add("hearing", createObjectBuilder().add("caseId", sendingSheetCompleted.getHearing().getCaseId().toString())).build());
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SendingSheetCompleted.class)).thenReturn(sendingSheetCompleted);
        final ListingCase listingCase = new ListingCase(hearing.getCaseId(), null, null);
        when(sendingSheetCompleteToListingCaseConverter.convert(sendingSheetCompleted)).thenReturn(listingCase);

        //when
        progressionEventProcessor.publishSendingSheetCompletedEvent(jsonEnvelope);

        //then
        verify(listHearingService).startProcess(captor.capture());

        Assert.assertThat(captor.getValue().get(CASE_ID_FIELD), IsEqual.equalTo(listingCase.getCaseId()));
        Assert.assertThat((ListingCase) captor.getValue().get(SEND_CASE_FORLISTING_PAYLOAD), IsEqual.equalTo(listingCase));
        Assert.assertThat(captor.getValue().get(USER_ID), IsEqual.equalTo(ProcessMapConstant.USER_ID));
        Assert.assertThat(UUID.fromString(captor.getValue().get(HEARING_ID).toString()).toString().length(), IsEqual.equalTo(36));
        Assert.assertThat(captor.getValue().get(WHEN), IsEqual.equalTo("Sending Sheet Complete"));
        Assert.assertThat(((List<String>) captor.getValue().get(PLEA)).contains(GUILTY), IsEqual.equalTo(false));
    }

    @Test
    public void shouldProcessEventRaiseSendCaseForListing_when_Defendant_Guilty_Is_Null() throws Exception {
        //given
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.randomUUID());
        final Defendant defendant = new Defendant();
        final Offence offenceOne = new Offence();
        final Offence offenceTwo = new Offence();
        defendant.setOffences(Arrays.asList(offenceOne, offenceTwo));
        hearing.setDefendants(Arrays.asList(defendant));
        sendingSheetCompleted.setHearing(hearing);
        final JsonEnvelope jsonEnvelope = JsonHelper.createJsonEnvelope(
                JsonHelper.createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED, UUID.randomUUID().toString(), ProcessMapConstant.USER_ID),
                Json.createObjectBuilder().add("hearing", createObjectBuilder().add(CASE_ID_FIELD, sendingSheetCompleted.getHearing().getCaseId().toString())).build());
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SendingSheetCompleted.class)).thenReturn(sendingSheetCompleted);
        final ListingCase listingCase = new ListingCase(hearing.getCaseId(), null, null);
        when(sendingSheetCompleteToListingCaseConverter.convert(sendingSheetCompleted)).thenReturn(listingCase);

        //when
        progressionEventProcessor.publishSendingSheetCompletedEvent(jsonEnvelope);

        //then
        verify(listHearingService).startProcess(captor.capture());

        Assert.assertThat(captor.getValue().get(CASE_ID_FIELD), IsEqual.equalTo(listingCase.getCaseId()));
        Assert.assertThat((ListingCase) captor.getValue().get(SEND_CASE_FORLISTING_PAYLOAD), IsEqual.equalTo(listingCase));
        Assert.assertThat(captor.getValue().get(USER_ID), IsEqual.equalTo(ProcessMapConstant.USER_ID));
        Assert.assertThat(UUID.fromString(captor.getValue().get(HEARING_ID).toString()).toString().length(), IsEqual.equalTo(36));
        Assert.assertThat(captor.getValue().get(WHEN), IsEqual.equalTo("Sending Sheet Complete"));
        Assert.assertThat(((List<String>) captor.getValue().get(PLEA)).contains(GUILTY), IsEqual.equalTo(false));
    }

}
