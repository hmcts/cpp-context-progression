package uk.gov.moj.cpp.nows.event.listener;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class NowsRequestedEventListenerTest {

    @Mock
    private CourtDocumentRepository repository;

    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Mock
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @InjectMocks
    private NowsRequestedEventListener nowsRequestedEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        //stringToJsonObjectConverter = new StringToJsonObjectConverter();
    }


    @Test
    public void testNowsMaterialStatusUpdated() {
        final UUID materialId = UUID.randomUUID();
        final UUID courtDocumentId = UUID.randomUUID();
        final NowsMaterialStatusUpdated update = NowsMaterialStatusUpdated.nowsMaterialStatusUpdated()
                .withStatus("generated")
                .withDetails(
                        MaterialDetails.materialDetails()
                                .withMaterialId(materialId)
                                .withFileId(UUID.randomUUID())
                                .withHearingId(UUID.randomUUID())
                                .build()
                ).build();
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
        when(repository.findBy(courtDocumentId)).thenReturn(courtDocumentEntity);
        nowsRequestedEventListener.nowsMaterialStatusUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.nows-material-status-updated"),
                objectToJsonObjectConverter.convert(update)));

        final ArgumentCaptor<CourtDocumentEntity> courtDocumentsSavedCaptor = ArgumentCaptor.forClass(CourtDocumentEntity.class);
        verify(this.repository).save(courtDocumentsSavedCaptor.capture());
        final CourtDocumentEntity savedEntity = courtDocumentsSavedCaptor.getValue();
        final JsonObject jsonPayload = Json.createReader(new StringReader(savedEntity.getPayload())).readObject();
        final CourtDocument courtDocumentSaved = jsonObjectToObjectConverter.convert(jsonPayload, CourtDocument.class);
        assertThat(courtDocumentSaved.getCourtDocumentId(), is(originalCourtDocument.getCourtDocumentId()));
        assertThat(courtDocumentSaved.getMaterials().size(), is(originalCourtDocument.getMaterials().size()));
        final Material materialSaved = courtDocumentSaved.getMaterials().get(0);
        assertThat(materialSaved.getGenerationStatus(), is(update.getStatus()));


    }

}
