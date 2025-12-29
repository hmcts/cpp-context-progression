package uk.gov.moj.cpp.nows.event.listener;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.io.StringReader;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowNotificationGeneratedEventListenerTest {

    private static final String STATUS = "status";
    private static final String MATERIAL_ID = "materialId";
    private static final String HEARING_ID = "hearingId";

    @Mock
    private CourtDocumentRepository courtDocumentRepository;
    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;
    @InjectMocks
    private NowNotificationGeneratedEventListener nowNotificationGeneratedEventListener;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSaveNowNotificationGenerated() {
        final UUID materialId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(STATUS, "generated")
                .add(HEARING_ID, UUID.randomUUID().toString())
                .build();
        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setCourtDocumentId(courtDocumentId);
        when(courtDocumentMaterialRepository.findBy(materialId)).thenReturn(courtDocumentMaterialEntity);
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        final CourtDocument originalCourtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeId(UUID.randomUUID())
                .withMaterials(asList(Material.material()
                        .withId(materialId)
                        .build()))
                .build();
        courtDocumentEntity.setPayload(
                objectToJsonObjectConverter.convert(originalCourtDocument).toString()
        );
        when(courtDocumentRepository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);
        nowNotificationGeneratedEventListener.nowNotificationGenerated(envelopeFrom(metadataWithRandomUUID("progression.event.now-notification-generated"),
                objectToJsonObjectConverter.convert(payload)));

        final ArgumentCaptor<CourtDocumentEntity> courtDocumentsSavedCaptor = ArgumentCaptor.forClass(CourtDocumentEntity.class);
        verify(this.courtDocumentRepository).save(courtDocumentsSavedCaptor.capture());
        final CourtDocumentEntity savedEntity = courtDocumentsSavedCaptor.getValue();
        final JsonObject jsonPayload = JsonObjects.createReader(new StringReader(savedEntity.getPayload())).readObject();
        final CourtDocument courtDocumentSaved = jsonObjectToObjectConverter.convert(jsonPayload, CourtDocument.class);
        assertThat(courtDocumentSaved.getCourtDocumentId(), is(originalCourtDocument.getCourtDocumentId()));
        assertThat(courtDocumentSaved.getMaterials().size(), is(originalCourtDocument.getMaterials().size()));
        final Material materialSaved = courtDocumentSaved.getMaterials().get(0);
        assertThat(materialSaved.getGenerationStatus(), is(payload.getString(STATUS)));
    }
}
