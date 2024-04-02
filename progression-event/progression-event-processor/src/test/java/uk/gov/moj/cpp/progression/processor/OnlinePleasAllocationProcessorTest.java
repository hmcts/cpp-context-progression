package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.helper.TestHelper.getPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.hearing.json.schema.event.AllocationPleasAdded;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleasAllocationDetails;

import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnlinePleasAllocationProcessorTest {
    private static final String ALLOCATION_PLEAS_PAYLOAD = "public.defence.allocation-pleas.json";
    private static final String PROGRESSION_COMMAND_ALLOCATION_PLEAS_ADDED = "progression.command.add-online-plea-allocation";
    private static final String PROGRESSION_COMMAND_ALLOCATION_PLEAS_UPDATED = "progression.command.update-online-plea-allocation";
    private static final String PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED = "public.defence.allocation-pleas-added";
    private static final String PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED = "public.defence.allocation-pleas-updated";

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Mock
    private Sender sender;
    @InjectMocks
    private OnlinePleasAllocationProcessor allocationProcessor;
    @Mock
    private FeatureControlGuard featureControlGuard;

    @Test
    public void shouldProcessAllocationPleasAddedWhenFeatureIsOn() {
        final JsonObject allocationPleasPayload = getPayload(ALLOCATION_PLEAS_PAYLOAD);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED),
                allocationPleasPayload);

        when(featureControlGuard.isFeatureEnabled("OPA")).thenReturn(true);
        allocationProcessor.defenceOnlinePleaAllocationAdded(event);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> envelope = envelopeArgumentCaptor.getValue();

        verifyOnlinePleaPayloadContents(envelope, allocationPleasPayload, PROGRESSION_COMMAND_ALLOCATION_PLEAS_ADDED);
    }

    @Test
    public void shouldNotProcessAllocationPleasAddedWhenFeatureIsOff() {
        final JsonObject allocationPleasPayload = getPayload(ALLOCATION_PLEAS_PAYLOAD);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED),
                allocationPleasPayload);

        when(featureControlGuard.isFeatureEnabled("OPA")).thenReturn(false);
        allocationProcessor.defenceOnlinePleaAllocationAdded(event);

        verifyZeroInteractions(sender);
    }

    @Test
    public void shouldProcessAllocationPleasUpdatedWhenFeatureIsOn() {
        final JsonObject allocationPleasPayload = getPayload(ALLOCATION_PLEAS_PAYLOAD);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED),
                allocationPleasPayload);
        when(featureControlGuard.isFeatureEnabled("OPA")).thenReturn(true);
        allocationProcessor.defenceOnlinePleaAllocationUpdated(event);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> envelope = envelopeArgumentCaptor.getValue();

        verifyOnlinePleaPayloadContents(envelope, allocationPleasPayload, PROGRESSION_COMMAND_ALLOCATION_PLEAS_UPDATED);
    }

    @Test
    public void shouldNotProcessAllocationPleasUpdatedWhenFeatureIsOff() {
        final JsonObject allocationPleasPayload = getPayload(ALLOCATION_PLEAS_PAYLOAD);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED),
                allocationPleasPayload);
        when(featureControlGuard.isFeatureEnabled("OPA")).thenReturn(false);
        allocationProcessor.defenceOnlinePleaAllocationUpdated(event);

        verifyZeroInteractions(sender);
    }

    private void verifyOnlinePleaPayloadContents(final Envelope<JsonObject> envelope, final JsonObject onlinePleJson, final String event) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataFrom(envelope.metadata()),
                envelope.payload());

        final PleasAllocationDetails pleasAllocation = jsonObjectToObjectConverter.convert(onlinePleJson, AllocationPleasAdded.class).getPleasAllocation();
        final Stream<JsonEnvelope> envelopeStream = Stream.of(jsonEnvelope);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(event),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.pleasAllocation.allocationId", CoreMatchers.is(pleasAllocation.getAllocationId().toString())),
                                withJsonPath("$.pleasAllocation.caseId", CoreMatchers.is(pleasAllocation.getCaseId().toString())),
                                withJsonPath("$.pleasAllocation.defendantId", CoreMatchers.is(pleasAllocation.getDefendantId().toString())),
                                withJsonPath("$.pleasAllocation.offencePleas[0].indicatedPlea", CoreMatchers.is(pleasAllocation.getOffencePleas().get(0).getIndicatedPlea())),
                                withJsonPath("$.pleasAllocation.offencePleas[0].offenceId", CoreMatchers.is(pleasAllocation.getOffencePleas().get(0).getOffenceId().toString())),
                                withJsonPath("$.pleasAllocation.offencePleas[0].pleaDate", CoreMatchers.is(pleasAllocation.getOffencePleas().get(0).getPleaDate().toString())),
                                withJsonPath("$.pleasAllocation.offencePleas[1].indicatedPlea", CoreMatchers.is(pleasAllocation.getOffencePleas().get(1).getIndicatedPlea())),
                                withJsonPath("$.pleasAllocation.offencePleas[1].offenceId", CoreMatchers.is(pleasAllocation.getOffencePleas().get(1).getOffenceId().toString())),
                                withJsonPath("$.pleasAllocation.offencePleas[1].pleaDate", CoreMatchers.is(pleasAllocation.getOffencePleas().get(1).getPleaDate().toString())))))
        ));
    }
}
