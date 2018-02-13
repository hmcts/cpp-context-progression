package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
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
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope messageToPublish;

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
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.sending-sheet-completed",
                createObjectBuilder().add("hearing", createObjectBuilder()
                        .add("caseId", CASE_ID)).build());

        // when
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


}
