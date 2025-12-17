package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DefendantUpdateDifferenceService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateDefendantsWithMatchedRequestedProcessorTest {

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @InjectMocks
    private UpdateDefendantsWithMatchedRequestedProcessor eventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private Requester requester;
    @Mock
    private JsonEnvelope responseEnvelope;
    @Mock
    private DefendantUpdateDifferenceService defendantUpdateDifferenceService;
    @Mock
    private ProgressionService progressionService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleUpdateDefendantWithMatchedRequestedEvent() {


        final UUID originalDefendantId = UUID.randomUUID();
        final UUID originalProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId = UUID.randomUUID();
        final Defendant defendant = Defendant.defendant()
                .withId(originalDefendantId)
                .build();
        final DefendantUpdate originalDefendantNextVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .build();
        final ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2 eventContent = ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.prosecutionCaseUpdateDefendantsWithMatchedRequestedV2()
                .withMatchedDefendants(Arrays.asList(Defendant.defendant()
                        .withId(matchedDefendantId)
                        .withProsecutionCaseId(matchedDefendantProsecutionCaseId)
                        .build()))
                .withDefendant(defendant)
                .withDefendantUpdate(originalDefendantNextVersion)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-case-update-defendants-with-matched-requested-v2"),
                objectToJsonObjectConverter.convert(eventContent));
        final DefendantUpdate matchedDefendantNextVersion = DefendantUpdate.defendantUpdate()
                .withId(matchedDefendantId)
                .withProsecutionCaseId(matchedDefendantProsecutionCaseId)
                .build();

        when(defendantUpdateDifferenceService.calculateDefendantUpdate(
                any(),//eq(originalDefendantPreviousVersion),
                any(),//eq(originalDefendantNextVersion),
                any()//eq(matchedDefendantPreviousVersion)
        )).thenReturn(matchedDefendantNextVersion);
        // run
        eventProcessor.handleUpdateDefendantWithMatchedRequestedEvent(jsonEnvelope);

        // verify

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = envelopeArgumentCaptor.getAllValues();

        assertThat(allValues.get(0), jsonEnvelope(
                metadata().withName("progression.command.update-defendant-for-prosecution-case"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendant.id", is(originalDefendantId.toString())),
                        withJsonPath("$.id", is(originalDefendantId.toString())),
                        withJsonPath("$.prosecutionCaseId", is(originalProsecutionCaseId.toString()))
                ))));

        assertThat(allValues.get(1), jsonEnvelope(
                metadata().withName("progression.command.update-defendant-for-prosecution-case"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendant.id", is(matchedDefendantId.toString())),
                        withJsonPath("$.id", is(matchedDefendantId.toString())),
                        withJsonPath("$.prosecutionCaseId", is(matchedDefendantProsecutionCaseId.toString()))
                ))));
    }

    @Test
    public void handleUpdateDefendantWithMatchedRequestedEventForOnlyOneDefendant() {


        final UUID originalDefendantId = UUID.randomUUID();
        final UUID originalProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId = UUID.randomUUID();
        final DefendantUpdate originalDefendantNextVersion = DefendantUpdate.defendantUpdate()
                .withId(originalDefendantId)
                .withProsecutionCaseId(originalProsecutionCaseId)
                .build();
        final ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2 eventContent = ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.prosecutionCaseUpdateDefendantsWithMatchedRequestedV2()
                .withMatchedDefendants(asList())
                .withDefendantUpdate(originalDefendantNextVersion)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-case-update-defendants-with-matched-requested-v2"),
                objectToJsonObjectConverter.convert(eventContent));
        final DefendantUpdate matchedDefendantNextVersion = DefendantUpdate.defendantUpdate()
                .withId(matchedDefendantId)
                .withProsecutionCaseId(matchedDefendantProsecutionCaseId)
                .build();

        // run
        eventProcessor.handleUpdateDefendantWithMatchedRequestedEvent(jsonEnvelope);

        // verify

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final List<JsonEnvelope> allValues = envelopeArgumentCaptor.getAllValues();

        assertThat(allValues.get(0), jsonEnvelope(
                metadata().withName("progression.command.update-defendant-for-prosecution-case"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendant.id", is(originalDefendantId.toString())),
                        withJsonPath("$.id", is(originalDefendantId.toString())),
                        withJsonPath("$.prosecutionCaseId", is(originalProsecutionCaseId.toString()))
                ))));

    }
}
