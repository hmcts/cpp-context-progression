package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;
import uk.gov.moj.cpp.progression.events.UnlinkedCases;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonArray;
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
public class UnlinkCasesEventProcessorTest {
    @Mock
    private Sender sender;

    @InjectMocks
    private UnlinkCasesEventProcessor processor;

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
    public void processUnlinkCases() {
        final UUID leadCaseId = UUID.randomUUID();
        final String leadCaseUrn = "caseUrn1";
        final UUID caseId = UUID.randomUUID();
        final String caseUrn = "caseUrn2";
        final UUID linkGroupId = UUID.randomUUID();

        final CasesUnlinked event = CasesUnlinked.casesUnlinked()
                .withProsecutionCaseId(leadCaseId)
                .withProsecutionCaseUrn(leadCaseUrn)
                .withUnlinkedCases(Arrays.asList(UnlinkedCases.unlinkedCases()
                        .withCaseId(caseId)
                        .withCaseUrn(caseUrn)
                        .withLinkGroupId(linkGroupId)
                        .build()))
                .build();

        final JsonObject casesUnlinkedPayload = objectToJsonObjectConverter.convert(event);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.cases-unlinked"),
                casesUnlinkedPayload);

        processor.casesUnlinked(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-linked"));

        final JsonObject actualPayload = publicEvent.payload();
        assertThat(actualPayload.getString("linkActionType"), equalTo("UNLINK"));
        final JsonArray caseArray = actualPayload.getJsonArray("cases");
        assertThat(caseArray.size(), is(2));

        final JsonObject link1 = caseArray.getJsonObject(0);
        assertThat(link1.getString("caseId"), equalTo(leadCaseId.toString()));
        assertThat(link1.getString("caseUrn"), equalTo(leadCaseUrn));
        final JsonArray linkedToCasesArray1 = link1.getJsonArray("linkedToCases");
        assertThat(linkedToCasesArray1.size(), is(1));
        final JsonObject linkedToCase1 = linkedToCasesArray1.getJsonObject(0);
        assertThat(linkedToCase1.getString("caseId"), equalTo(caseId.toString()));
        assertThat(linkedToCase1.getString("caseUrn"), equalTo(caseUrn));



        final JsonObject link2 = caseArray.getJsonObject(1);
        assertThat(link2.getString("caseId"), equalTo(caseId.toString()));
        assertThat(link2.getString("caseUrn"), equalTo(caseUrn));
        final JsonArray linkedToCasesArray2 = link2.getJsonArray("linkedToCases");
        assertThat(linkedToCasesArray2.size(), is(1));
        final JsonObject linkedToCase2 = linkedToCasesArray2.getJsonObject(0);
        assertThat(linkedToCase2.getString("caseId"), equalTo(leadCaseId.toString()));
        assertThat(linkedToCase2.getString("caseUrn"), equalTo(leadCaseUrn));
    }
}
