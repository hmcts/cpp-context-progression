package uk.gov.moj.cpp.progression.processor;

import static java.util.List.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.events.LastCasesToBeMovedFromGroupRejected;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseRemoveFromGroupCaseProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private CaseRemoveFromGroupCaseProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @BeforeEach
    void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldRepublishLastCaseToBeRemovedFromGroupCasesRejectedPublicly() {
        final UUID groupId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final LastCaseToBeRemovedFromGroupCasesRejected rejected = LastCaseToBeRemovedFromGroupCasesRejected.lastCaseToBeRemovedFromGroupCasesRejected()
                .withGroupId(groupId)
                .withCaseId(caseId)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(rejected);
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("progression.event.last-case-to-be-removed-from-group-cases-rejected"),
                payload);

        processor.processEvent(requestMessage);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> published = envelopeCaptor.getValue();
        assertThat(published.metadata().name(), is("public.progression.remove-last-case-in-group-cases-rejected"));
        assertThat(published.payload().getString("groupId"), is(groupId.toString()));
        assertThat(published.payload().getString("caseId"), is(caseId.toString()));
    }

    @Test
    void shouldRepublishLastCasesToBeMovedFromGroupRejectedPublicly() {
        final UUID groupId = UUID.randomUUID();
        final UUID caseId1 = UUID.randomUUID();
        final UUID caseId2 = UUID.randomUUID();

        final LastCasesToBeMovedFromGroupRejected rejected = LastCasesToBeMovedFromGroupRejected.lastCasesToBeMovedFromGroupRejected()
                .withGroupId(groupId)
                .withCaseIds(of(caseId1, caseId2))
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(rejected);
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("progression.event.last-cases-to-be-moved-from-group-rejected"),
                payload);

        processor.processMoveRejectedEvent(requestMessage);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> published = envelopeCaptor.getValue();
        assertThat(published.metadata().name(), is("public.progression.move-last-cases-from-group-rejected"));
        assertThat(published.payload().getString("groupId"), is(groupId.toString()));
        assertThat(published.payload().getJsonArray("caseIds").getString(0), is(caseId1.toString()));
        assertThat(published.payload().getJsonArray("caseIds").getString(1), is(caseId2.toString()));
    }
}
