package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;

import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

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
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void addConvictionDate() throws Exception {

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.convictionDateAdded()
                .withCaseId(UUID.randomUUID())
                .withOffenceId(UUID.randomUUID())
                .withConvictionDate(PAST_LOCAL_DATE.next())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.offence-conviction-date-changed"),
                objectToJsonObjectConverter.convert(convictionDateAdded));

        this.eventProcessor.handleHearingConvictionDateChangedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("progression.command.add-conviction-date"),
                payloadIsJson(allOf(withJsonPath("$.caseId", is(convictionDateAdded.getCaseId().toString())),
                        withJsonPath("$.offenceId", is(convictionDateAdded.getOffenceId().toString())),
                        withJsonPath("$.convictionDate", is(convictionDateAdded.getConvictionDate().toString()))))));

    }

    @Test
    public void removeConvictionDate() throws Exception {

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.convictionDateRemoved()
                .withCaseId(UUID.randomUUID())
                .withOffenceId(UUID.randomUUID()).build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.offence-conviction-date-removed"),
                objectToJsonObjectConverter.convert(convictionDateRemoved));

        this.eventProcessor.handleHearingConvictionDateRemovedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(),
                jsonEnvelope(metadata().withName("progression.command.remove-conviction-date"),
                        payloadIsJson(allOf(withJsonPath("$.caseId", is(convictionDateRemoved.getCaseId().toString())),
                                withJsonPath("$.offenceId", is(convictionDateRemoved.getOffenceId().toString()))))));

    }

}