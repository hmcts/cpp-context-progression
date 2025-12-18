package uk.gov.moj.cpp.progression.processor;


import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.processor.FinancialMeansProcessor.MATERIAL_COMMAND_DELETE_MATERIAL;
import static uk.gov.moj.cpp.progression.processor.FinancialMeansProcessor.PUBLIC_PROGRESSION_EVENTS_DEFENDANT_FINANCIAL_MEANS_DELETED;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FinancialMeansProcessorTest {

    @InjectMocks
    private FinancialMeansProcessor eventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Mock
    private Sender sender;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void shouldHandleFinancialMeansDeletedEventAndSendCommandToDeleteMaterials() {

        final String defendantId = "50770ba9-37ea-4713-8cab-fe5bf1202716";
        final String materialId1 = "M001";
        final String materialId2 = "M002";

        //Given
        final JsonEnvelope requestMessage = formRequest(defendantId, materialId1, materialId2);

        //When
        eventProcessor.deleteFinancialMeans(requestMessage);

        //Then
        verify(sender, times(3)).send(envelopeCaptor.capture());
        assertCommandsAndEventsGenerated(requestMessage, defendantId, materialId1, materialId2);
    }

    private void assertCommandsAndEventsGenerated(final JsonEnvelope requestMessage, final String defendantId, final String materialId1, final String materialId2) {

        List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(MATERIAL_COMMAND_DELETE_MATERIAL));
        assertEquals(materialId1, commands.get(0).payload().getString("materialId"));

        assertThat(commands.get(1).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(MATERIAL_COMMAND_DELETE_MATERIAL));
        assertEquals(materialId2, commands.get(1).payload().getString("materialId"));

        assertThat(commands.get(2).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_PROGRESSION_EVENTS_DEFENDANT_FINANCIAL_MEANS_DELETED));
        assertEquals(defendantId, commands.get(2).payload().getString("defendantId"));

    }

    private JsonEnvelope formRequest(final String defendantId, final String materialId1, final String materialId2) {
        final JsonObject requestPayload = createObjectBuilder()
                .add("defendantId", defendantId)
                .add("materialIds", createArrayBuilder().add(materialId1).add(materialId2)).build();

        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.financial-means-deleted"),
                requestPayload);
    }


}
