package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;

import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;

@RunWith(MockitoJUnitRunner.class)
public class ConvictionDateEventProcessorTest {

    @InjectMocks
    private ConvictionDateEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope responseEnvelope;

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
    public void shouldHandleDefendantAddedEventMessage() throws Exception {

        ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.builder().withCaseId(UUID.randomUUID())
                .withOffenceId(UUID.randomUUID()).withConvictionDate(PAST_LOCAL_DATE.next()).build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.offence-conviction-date-changed"),
                objectToJsonObjectConverter.convert(convictionDateAdded));

        this.eventProcessor.handleHearingOffenceConvictionDateChangedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("progression.command.offence-conviction-date-changed"),
                payloadIsJson(allOf(withJsonPath("$.caseId", is(convictionDateAdded.getCaseId().toString())),
                        withJsonPath("$.offenceId", is(convictionDateAdded.getOffenceId().toString())),
                        withJsonPath("$.convictionDate", is(convictionDateAdded.getConvictionDate().toString()))))));

    }

    @Test
    public void shouldHandleDefendantRemovedEventMessage() throws Exception {

        ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.builder().withCaseId(UUID.randomUUID())
                .withOffenceId(UUID.randomUUID()).build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.offence-conviction-date-removed"),
                objectToJsonObjectConverter.convert(convictionDateRemoved));

        this.eventProcessor.handleHearingOffenceConvictionDateRemovedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(),
                jsonEnvelope(metadata().withName("progression.command.offence-conviction-date-removed"),
                        payloadIsJson(allOf(withJsonPath("$.caseId", is(convictionDateRemoved.getCaseId().toString())),
                                withJsonPath("$.offenceId", is(convictionDateRemoved.getOffenceId().toString()))))));

    }

}