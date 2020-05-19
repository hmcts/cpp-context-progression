package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.SEARCH_RESULTS;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        leadCaseId = UUID.randomUUID();
        leadCaseUrn = "caseUrn1";
        caseIdToLink = UUID.randomUUID();
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

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of(Json.createObjectBuilder().add(SEARCH_RESULTS, Json.createArrayBuilder().build()).build()));
        processor.handleMergeCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertTrue(publicEvent.payload().getString("linkResponseResults").equals(REFERENCE_NOT_FOUND.toString()));

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

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(SEARCH_RESULTS, Json.createArrayBuilder().add(Json.createObjectBuilder().add(CASE_ID, leadCaseId.toString())).build()).build()
        ));
        processor.handleMergeCasesValidations(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertTrue(publicEvent.payload().getString("linkResponseResults").equals(REFERENCE_NOT_VALID.toString()));

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

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(SEARCH_RESULTS, Json.createArrayBuilder().add(Json.createObjectBuilder().add(CASE_ID, caseIdToLink.toString()).add("reference", caseUrnToLink)).build()).build()
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
        assertTrue(commandEnvelope.payload().getString("prosecutionCaseId").equals(leadCaseId.toString()));
        assertTrue(commandEnvelope.payload().getJsonArray("casesToLink").getJsonObject(0).getJsonArray("caseUrns").getString(0).equals(caseUrnToLink));

        final Envelope<JsonObject> caseLinkedEnvelope = allEvents.get(1);
        assertThat(caseLinkedEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-linked"));
        assertThat(caseLinkedEnvelope.payload().getJsonArray("cases").size(), is(2));

        final Envelope<JsonObject> responseEnvelope = allEvents.get(2);
        assertThat(responseEnvelope.metadata(), withMetadataEnvelopedFrom(requestMessage).withName("public.progression.link-cases-response"));
        assertTrue(responseEnvelope.payload().getString("linkResponseResults").equals("SUCCESS"));

    }
}
