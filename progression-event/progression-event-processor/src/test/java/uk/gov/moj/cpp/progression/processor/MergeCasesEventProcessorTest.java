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
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.MERGED_CASES;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.ValidateLinkCases;
import uk.gov.moj.cpp.progression.events.ValidateMergeCases;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
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
public class MergeCasesEventProcessorTest {
    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private MergeCasesEventProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    private UUID leadCaseId;
    private String leadCaseUrn;
    private UUID caseIdToLink;
    private String caseUrnToLink;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        leadCaseId = randomUUID();
        leadCaseUrn = "caseUrn1";
        caseIdToLink = randomUUID();
        caseUrnToLink = "caseUrn2";

    }

    @Test
    public void shouldRaiseReferenceNotFound() {

        final ValidateMergeCases event = ValidateMergeCases.validateMergeCases()
                .withProsecutionCaseId(leadCaseId)
                .withCaseUrns(Arrays.asList(caseUrnToLink))
                .build();

        final JsonObject validatePayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-link-cases"),
                validatePayload);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(Json.createObjectBuilder().build()));
        processor.handleMergeCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertEquals(publicEvent.payload().getString("linkResponseResults"), REFERENCE_NOT_FOUND.toString());

    }

    @Test
    public void shouldRaiseReferenceNotValid() {

        final ValidateMergeCases event = ValidateMergeCases.validateMergeCases()
                .withProsecutionCaseId(leadCaseId)
                .withCaseUrns(Arrays.asList(leadCaseUrn))
                .build();

        final JsonObject validatePayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-link-cases"),
                validatePayload);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(CASE_ID, leadCaseId.toString()).build()
        ));
        processor.handleMergeCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertEquals(publicEvent.payload().getString("linkResponseResults"), REFERENCE_NOT_VALID.toString());

    }

    @Test
    public void shouldProcessValidateCases() {

        final ValidateLinkCases event = ValidateLinkCases.validateLinkCases()
                .withProsecutionCaseId(leadCaseId)
                .withCaseUrns(Arrays.asList(caseUrnToLink))
                .build();

        final JsonObject casesUnlinkedPayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.validate-link-cases"),
                casesUnlinkedPayload);

        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(CASE_ID, randomUUID().toString()).build()
        ));
        when(progressionService.searchLinkedCases(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(MERGED_CASES, Json.createArrayBuilder().build()).build()
        ));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().add("id", leadCaseId.toString()).add("prosecutionCaseIdentifier", Json.createObjectBuilder().add("caseURN", leadCaseUrn)
                ).build()).build()
        ));
        processor.handleMergeCasesValidations(requestMessage);
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
