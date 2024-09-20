package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilFeesUpdatedProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private CivilFeesUpdatedProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void processCaseNotesAdded() {
        List<CivilFees> feeList = new ArrayList<CivilFees>();
        CivilFees civilFees = new CivilFees(UUID.randomUUID(), FeeStatus.OUTSTANDING, FeeType.INITIAL,"Ref001");
        feeList.add(civilFees);

        final CivilFeesUpdated civilFeesUpdated = CivilFeesUpdated.civilFeesUpdated()
                .withCaseId(UUID.randomUUID())
                .withCivilFees(feeList)
                .build();

        final JsonObject caseNoteAddedPayload = objectToJsonObjectConverter.convert(civilFeesUpdated);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.civil-fees-updated"),
                caseNoteAddedPayload);

        processor.processCivilFees(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.civil-fees-response"));
        JsonObject actualPayload = publicEvent.payload();
        assertThat(actualPayload.getString("civilFeeResults"), equalTo("SUCCESS"));
    }
}
