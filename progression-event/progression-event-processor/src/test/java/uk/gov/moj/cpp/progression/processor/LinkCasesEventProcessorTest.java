package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.events.LinkResponseResults.REFERENCE_NOT_FOUND;
import static uk.gov.moj.cpp.progression.events.LinkResponseResults.REFERENCE_NOT_VALID;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.LINKED_CASES;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.ValidateLinkCases;
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
public class LinkCasesEventProcessorTest {
    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private LinkCasesEventProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldRaiseReferenceNotFound() {
        final UUID leadCaseId = randomUUID();
        final String caseUrnToLink = "caseUrn2";

        final ValidateLinkCases event = ValidateLinkCases.validateLinkCases()
                .withProsecutionCaseId(leadCaseId)
                .withCaseUrns(Arrays.asList(caseUrnToLink))
                .build();

        final JsonObject casesUnlinkedPayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-link-cases"),
                casesUnlinkedPayload);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(JsonObjects.createObjectBuilder().build()));
        processor.handleLinkCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertEquals(publicEvent.payload().getString("linkResponseResults"), REFERENCE_NOT_FOUND.toString());

    }

    @Test
    public void shouldRaiseReferenceNotValid() {
        final UUID leadCaseId = randomUUID();
        final String leadCaseUrn = "caseUrn2";

        final ValidateLinkCases event = ValidateLinkCases.validateLinkCases()
                .withProsecutionCaseId(leadCaseId)
                .withCaseUrns(Arrays.asList(leadCaseUrn))
                .build();

        final JsonObject validatePayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-link-cases"),
                validatePayload);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(
                JsonObjects.createObjectBuilder().add(CASE_ID, leadCaseId.toString()).build()
        ));
        processor.handleLinkCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertEquals(publicEvent.payload().getString("linkResponseResults"), REFERENCE_NOT_VALID.toString());

    }

    @Test
    public void shouldProcessLinkCases() {
        final UUID leadCaseId = randomUUID();
        final String leadCaseUrn = "caseUrn1";
        final UUID caseIdToLink = randomUUID();
        final String caseUrnToLink = "caseUrn2";

        final ValidateLinkCases event = ValidateLinkCases.validateLinkCases()
                .withProsecutionCaseId(leadCaseId)
                .withCaseUrns(Arrays.asList(caseUrnToLink))
                .build();

        final JsonObject validatePayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-link-cases"),
                validatePayload);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(
                JsonObjects.createObjectBuilder().add(CASE_ID, randomUUID().toString()).build()
        ));
        when(progressionService.searchLinkedCases(any(), any())).thenReturn(Optional.of(
                JsonObjects.createObjectBuilder().add(LINKED_CASES, JsonObjects.createArrayBuilder().build()).build()
        ));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(
                JsonObjects.createObjectBuilder().add("prosecutionCase", JsonObjects.createObjectBuilder().add("id", leadCaseId.toString()).add("prosecutionCaseIdentifier", JsonObjects.createObjectBuilder().add("caseURN", leadCaseUrn)
                ).build()).build()
        ));
        processor.handleLinkCasesValidations(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());
        final List<Envelope<JsonObject>> allEvents = envelopeCaptor.getAllValues();
        final Envelope<JsonObject> commandEnvelope = allEvents.get(0);
        assertThat(commandEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("progression.command.link-cases"));
        assertEquals(commandEnvelope.payload().getString("prosecutionCaseId"), leadCaseId.toString());
        assertEquals(commandEnvelope.payload().getJsonArray("casesToLink").getJsonObject(0).getJsonArray("caseUrns").getString(0), caseUrnToLink);

        final Envelope<JsonObject> caseLinkedEnvelope = allEvents.get(1);
        assertThat(caseLinkedEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-linked"));
        assertThat(caseLinkedEnvelope.payload().getJsonArray("cases").size(), is(2));

        final Envelope<JsonObject> responseEnvelope = allEvents.get(2);
        assertThat(responseEnvelope.metadata(), withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertEquals("SUCCESS", responseEnvelope.payload().getString("linkResponseResults"));

    }
}
