package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.events.LinkResponseResults.REFERENCE_ALREADY_LINKED;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_URN;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.SPLIT_CASES;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.ValidateSplitCases;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SplitCasesEventProcessorTest {
    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private SplitCasesEventProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    private UUID caseId;
    private List<String> caseUrnsToSplit;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        caseId = UUID.randomUUID();
        caseUrnsToSplit = Arrays.asList("caseUrn/1", "caseUrn/2");
    }

    @Test
    public void shouldRaiseReferenceAlreadyLinked() {


        final ValidateSplitCases event = ValidateSplitCases.validateSplitCases()
                .withProsecutionCaseId(caseId)
                .withCaseUrns(caseUrnsToSplit)
                .build();

        final JsonObject validatePayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-split-cases"),
                validatePayload);

        when(progressionService.searchLinkedCases(any(), any())).thenReturn(Optional.of(JsonObjects.createObjectBuilder().add(SPLIT_CASES, JsonObjects.createArrayBuilder().add(
                JsonObjects.createObjectBuilder().add(CASE_URN, String.join(",", caseUrnsToSplit))).build()).build()));
        processor.handleSplitCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertTrue(publicEvent.payload().getString("linkResponseResults").equals(REFERENCE_ALREADY_LINKED.toString()));
        assertThat(publicEvent.payload().getJsonArray("invalidCaseUrns").size(), is(2));
        assertThat(publicEvent.payload().getJsonArray("invalidCaseUrns").get(0).toString(), is("\"caseUrn/1\""));
        assertThat(publicEvent.payload().getJsonArray("invalidCaseUrns").get(1).toString(), is("\"caseUrn/2\""));
    }

    @Test
    public void shouldProcessValidateCases() {
        final UUID caseId = UUID.randomUUID();
        final List<String> caseUrnsToSplit = Arrays.asList("caseUrn/1", "caseUrn/2");

        final ValidateSplitCases event = ValidateSplitCases.validateSplitCases()
                .withProsecutionCaseId(caseId)
                .withCaseUrns(caseUrnsToSplit)
                .build();

        final JsonObject validatePayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-split-cases"),
                validatePayload);

        when(progressionService.searchLinkedCases(any(), any())).thenReturn(Optional.of(
                JsonObjects.createObjectBuilder().add(SPLIT_CASES, JsonObjects.createArrayBuilder().build()).build()
        ));
        processor.handleSplitCasesValidations(requestMessage);

        verify(sender, times(2)).send(envelopeCaptor.capture());
        final List<Envelope<JsonObject>> publicEvents = envelopeCaptor.getAllValues();

        final Envelope<JsonObject> commandEnvelope = publicEvents.get(0);
        assertThat(commandEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("progression.command.link-cases"));
        assertTrue(commandEnvelope.payload().getString("prosecutionCaseId").equals(caseId.toString()));
        assertTrue(commandEnvelope.payload().getJsonArray("casesToLink").getJsonObject(0).getJsonArray("caseUrns").size() == 2);

        final Envelope<JsonObject> responseEnvelope = publicEvents.get(1);

        assertThat(responseEnvelope.metadata(), withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertTrue(responseEnvelope.payload().getString("linkResponseResults").equals("SUCCESS"));


    }
}
