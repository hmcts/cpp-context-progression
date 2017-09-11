package uk.gov.moj.cpp.progression.event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionEventProcessorTest {

    private static final String CASE_ID = UUID.randomUUID().toString();

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope messageToPublish;

    @InjectMocks
    private ProgressionEventProcessor progressionEventProcessor;

    @Test
    public void publishSentenceHearingAddedPublicEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.sentence-hearing-date-added", createObjectBuilder().add("caseId", CASE_ID).build());


        // when
        progressionEventProcessor.publishSentenceHearingAddedPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.sentence-hearing-date-added"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishSentenceHearingDateUpdatedPublicEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.sentence-hearing-date-updated", createObjectBuilder().add("caseId", CASE_ID).build());


        // when
        progressionEventProcessor.publishSentenceHearingUpdatedPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.sentence-hearing-date-updated"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishSentenceHearingUpdatedPublicEvent() {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.sentence-hearing-added", createObjectBuilder().add("caseId", CASE_ID).build());


        // when
        progressionEventProcessor.publishSentenceHearingIdAddedPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.sentence-hearing-added"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }

    @Test
    public void publishCaseAddedToCrownCourtPublicEvent() {
        // given
        final String CASE_PROGRESSION_ID = UUID.randomUUID().toString();
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.case-added-to-crown-court", createObjectBuilder().
                add("caseId", CASE_ID).
                add("caseProgressionId",CASE_PROGRESSION_ID).
                add("courtCentreId","LiverPool").
                add("status", CaseStatusEnum.INCOMPLETE.toString()).build());

        // when
        progressionEventProcessor.publishCaseAddedToCrownCourtPublicEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
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
        final String CASE_PROGRESSION_ID = UUID.randomUUID().toString();
        final JsonEnvelope event = EnvelopeFactory.createEnvelope("progression.events.case-already-exists-in-crown-court", createObjectBuilder().
                add("caseId", CASE_ID).build());

        // when
        progressionEventProcessor.publishCaseAlreadyExistsInCrownCourtEvent(event);

        // then
        final ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.events.case-already-exists-in-crown-court"),
                payloadIsJson(
                        withJsonPath(format("$.%s", "caseId"), equalTo(CASE_ID))
                )));
    }
}