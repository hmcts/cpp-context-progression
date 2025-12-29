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
import uk.gov.justice.core.courts.NowDocumentRequestToBeAcknowledged;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.core.courts.nowdocument.FinancialOrderDetails;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NowDocumentRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NowDocumentRequestRepository;

import java.io.StringReader;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class NowsRequestedEventListenerTest {

    @Mock
    private CourtDocumentRepository repository;

    @Mock
    private NowDocumentRequestRepository nowDocumentRequestRepository;

    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @InjectMocks
    private NowsRequestedEventListener nowsRequestedEventListener;

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
    public void shouldSaveNowsMaterialStatusUpdated() {
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
        final JsonObject jsonPayload = JsonObjects.createReader(new StringReader(savedEntity.getPayload())).readObject();
        final CourtDocument courtDocumentSaved = jsonObjectToObjectConverter.convert(jsonPayload, CourtDocument.class);
        assertThat(courtDocumentSaved.getCourtDocumentId(), is(originalCourtDocument.getCourtDocumentId()));
        assertThat(courtDocumentSaved.getMaterials().size(), is(originalCourtDocument.getMaterials().size()));
        final Material materialSaved = courtDocumentSaved.getMaterials().get(0);
        assertThat(materialSaved.getGenerationStatus(), is(update.getStatus()));


    }

    @Test
    public void shouldSaveNowDocumentRequested() {
        final UUID materialId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(materialId)
                .withHearingId(hearingId)
                .withNowContent(NowDocumentContent.nowDocumentContent()
                        .build())
                .build();

        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withMaterialId(materialId)
                .withNowDocumentRequest(nowDocumentRequest)
                .build();


        nowsRequestedEventListener.saveNowDocumentRequestedPayload(envelopeFrom(metadataWithRandomUUID("progression.event.now-document-requested"),
                objectToJsonObjectConverter.convert(nowDocumentRequested)));

        final ArgumentCaptor<NowDocumentRequestEntity> nowDocumentRequestedCaptor = ArgumentCaptor.forClass(NowDocumentRequestEntity.class);
        verify(this.nowDocumentRequestRepository).save(nowDocumentRequestedCaptor.capture());
        final NowDocumentRequestEntity savedNowDocumentRequestEntity = nowDocumentRequestedCaptor.getValue();
        final JsonObject jsonPayload = JsonObjects.createReader(new StringReader(savedNowDocumentRequestEntity.getPayload())).readObject();
        final NowDocumentRequest nowDocumentRequestSaved = jsonObjectToObjectConverter.convert(jsonPayload, NowDocumentRequest.class);

        assertThat(savedNowDocumentRequestEntity.getMaterialId(), is(materialId));
        assertThat(savedNowDocumentRequestEntity.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldSaveNowDocumentRequestToBeAcknowledged() {
        final UUID materialId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(materialId)
                .withHearingId(hearingId)
                .withRequestId(requestId.toString())
                .withNowContent(NowDocumentContent.nowDocumentContent()
                        .withFinancialOrderDetails(FinancialOrderDetails.financialOrderDetails()
                                .build())
                        .build())
                .build();

        final NowDocumentRequestToBeAcknowledged nowDocumentRequestToBeAcknowledged = NowDocumentRequestToBeAcknowledged.nowDocumentRequestToBeAcknowledged()
                .withMaterialId(materialId)
                .withNowDocumentRequest(nowDocumentRequest)
                .build();


        nowsRequestedEventListener.saveNowDocumentToBeAcknowledgedPayload(envelopeFrom(metadataWithRandomUUID("progression.event.now-document-request-to-be-acknowledged"),
                objectToJsonObjectConverter.convert(nowDocumentRequestToBeAcknowledged)));

        final ArgumentCaptor<NowDocumentRequestEntity> nowDocumentRequestedCaptor = ArgumentCaptor.forClass(NowDocumentRequestEntity.class);
        verify(this.nowDocumentRequestRepository).save(nowDocumentRequestedCaptor.capture());
        final NowDocumentRequestEntity savedNowDocumentRequestEntity = nowDocumentRequestedCaptor.getValue();
        final JsonObject jsonPayload = JsonObjects.createReader(new StringReader(savedNowDocumentRequestEntity.getPayload())).readObject();
        final NowDocumentRequest nowDocumentRequestSaved = jsonObjectToObjectConverter.convert(jsonPayload, NowDocumentRequest.class);

        assertThat(savedNowDocumentRequestEntity.getMaterialId(), is(materialId));
        assertThat(savedNowDocumentRequestEntity.getHearingId(), is(hearingId));
        assertThat(savedNowDocumentRequestEntity.getRequestId(), is(requestId));
    }

}
