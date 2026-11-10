package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.FinancialMeansDeleted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FinancialMeansListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private CourtDocumentRepository courtDocumentRepository;

    @Mock
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @InjectMocks
    private FinancialMeansListener financialMeansListener;

    @Captor
    private ArgumentCaptor<CourtDocumentEntity> argumentCaptorForCourtDocumentEntity;

    @Captor
    private ArgumentCaptor<CourtDocumentMaterialEntity> argumentCaptorForCourtDocumentMaterialEntity;

    @Test
    public void shouldHandleFinancialMeansDeletedEvent() {

        //Given
        final String defendantId = "50770ba9-37ea-4713-8cab-fe5bf1202716";
        final String caseId = "50770ba9-37ea-4713-8cab-fe5bf1202717";
        final String materialId = "M001";
        final UUID courtDocumentId1 = UUID.randomUUID();
        final UUID courtDocumentId2 = UUID.randomUUID();
        final UUID courtDocumentId3 = UUID.randomUUID();
        final JsonEnvelope requestMessage = formRequest(defendantId, caseId, materialId);

        final List<CourtDocumentEntity> courtDocumentEntities = createCourtDocumentEntities(courtDocumentId1, courtDocumentId2, courtDocumentId3);
        final List<CourtDocumentEntity> documentsWithFinancialMeans = courtDocumentEntities.stream().filter(CourtDocumentEntity::getContainsFinancialMeans).collect(toList());
        final List<CourtDocumentMaterialEntity> courtDocumentMaterialEntities = documentsWithFinancialMeans
                .stream().map(CourtDocumentEntity::getCourtDocumentId).map(this::getCourtDocumentMaterialEntity).collect(toList());
        //ensure that there are documents without financial means so that we can assert they do not get deleted
        assertThat(courtDocumentEntities.size(), greaterThan(documentsWithFinancialMeans.size()));

        when(courtDocumentRepository.findByProsecutionCaseIdAndDefendantId(newArrayList(UUID.fromString(caseId)), newArrayList(UUID.fromString(defendantId)))).thenReturn(courtDocumentEntities);
        when(courtDocumentMaterialRepository.findOptionalByCourtDocumentId(courtDocumentId1)).thenReturn(getCourtDocumentMaterialEntity(courtDocumentId1));
        when(courtDocumentMaterialRepository.findOptionalByCourtDocumentId(courtDocumentId3)).thenReturn(getCourtDocumentMaterialEntity(courtDocumentId3));

        final FinancialMeansDeleted financialMeansDeleted = FinancialMeansDeleted.financialMeansDeleted()
                .withCaseId(UUID.fromString(caseId))
                .withDefendantId(UUID.fromString(defendantId))
                .build();
        when(jsonObjectConverter.convert(any(), any())).thenReturn(financialMeansDeleted);

        //When
        financialMeansListener.deleteFinancialMeans(requestMessage);

        //Then
        verify(courtDocumentMaterialRepository, times(2)).remove(argumentCaptorForCourtDocumentMaterialEntity.capture());

        final List<UUID> removedCourtDocumentMaterialsDocId =
                argumentCaptorForCourtDocumentMaterialEntity
                        .getAllValues()
                        .stream()
                        .map(CourtDocumentMaterialEntity::getCourtDocumentId)
                        .collect(toList());

        assertThat(removedCourtDocumentMaterialsDocId, hasSize(documentsWithFinancialMeans.size()));
        assertThat(removedCourtDocumentMaterialsDocId, hasItems(courtDocumentId1, courtDocumentId3));

        verify(courtDocumentRepository, times(2)).remove(argumentCaptorForCourtDocumentEntity.capture());

        final List<UUID> removedCourtDocumentsDocId =
                argumentCaptorForCourtDocumentEntity
                        .getAllValues()
                        .stream()
                        .map(CourtDocumentEntity::getCourtDocumentId)
                        .collect(toList());

        assertThat(removedCourtDocumentsDocId, hasSize(documentsWithFinancialMeans.size()));
        assertThat(removedCourtDocumentsDocId, hasItems(courtDocumentId1, courtDocumentId3));
    }

    private CourtDocumentMaterialEntity getCourtDocumentMaterialEntity(final UUID courtDocumentId) {
        CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setCourtDocumentId(courtDocumentId);
        return courtDocumentMaterialEntity;
    }

    private List<CourtDocumentEntity> createCourtDocumentEntities(final UUID courtDocumentId1, final UUID courtDocumentId2, final UUID courtDocumentId3) {

        List<CourtDocumentEntity> courtDocumentEntities = new ArrayList<>();
        courtDocumentEntities.add(getCourtDocumentEntity(courtDocumentId1, true));
        courtDocumentEntities.add(getCourtDocumentEntity(courtDocumentId2, false));
        courtDocumentEntities.add(getCourtDocumentEntity(courtDocumentId3, true));
        return courtDocumentEntities;
    }


    private CourtDocumentEntity getCourtDocumentEntity(final UUID courtDocumentId, boolean containsFinancialMeans) {

        CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setContainsFinancialMeans(containsFinancialMeans);
        return courtDocumentEntity;
    }

    private JsonEnvelope formRequest(final String defendantId, final String caseId, final String materialId) {
        final JsonObject requestPayload = createObjectBuilder()
                .add("defendantId", defendantId)
                .add("caseId", caseId)
                .add("materialIds", createArrayBuilder().add(materialId))
                .build();

        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.financial-means-deleted"),
                requestPayload);
    }

}
