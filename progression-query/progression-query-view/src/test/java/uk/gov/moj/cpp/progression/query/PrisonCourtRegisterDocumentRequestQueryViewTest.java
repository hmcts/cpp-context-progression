package uk.gov.moj.cpp.progression.query;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrisonCourtRegisterDocumentRequestQueryViewTest {

    @Mock
    private PrisonCourtRegisterRepository prisonCourtRegisterRepository;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private PrisonCourtRegisterDocumentRequestQueryView prisonCourtRegisterDocumentRequestQueryView;

    @Test
    public void getPrisonCourtRegisterByCourtCentre() {
        final UUID fileId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.prison-court-register-document-by-court-centre").build(),
                JsonObjects.createObjectBuilder().add("courtCentreId", courtCentreId.toString()).build());

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCentreId);
        prisonCourtRegisterEntity.setFileId(fileId);
        prisonCourtRegisterEntity.setRecordedDate(LocalDate.now());

        final JsonObject transformedJsonEntity = JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("fileId", fileId.toString())
                .build();
        when(objectToJsonObjectConverter.convert(prisonCourtRegisterEntity)).thenReturn(transformedJsonEntity);
        when(prisonCourtRegisterRepository.findByCourtCentreId(courtCentreId)).thenReturn(Lists.newArrayList(prisonCourtRegisterEntity));

        final JsonEnvelope prisonCourtRegisterRequests = prisonCourtRegisterDocumentRequestQueryView.getPrisonCourtRegistersByCourtCentre(envelope);
        assertThat(prisonCourtRegisterRequests.payloadAsJsonObject().getJsonArray("prisonCourtRegisterDocumentRequests").size(), is(1));
        assertThat(prisonCourtRegisterRequests.payloadAsJsonObject().getJsonArray("prisonCourtRegisterDocumentRequests")
                .getJsonObject(0).getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(prisonCourtRegisterRequests.payloadAsJsonObject().getJsonArray("prisonCourtRegisterDocumentRequests")
                .getJsonObject(0).getString("fileId"), is(fileId.toString()));
    }
}
