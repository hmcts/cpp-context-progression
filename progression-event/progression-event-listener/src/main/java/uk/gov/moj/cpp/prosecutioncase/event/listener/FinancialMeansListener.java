package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.stream.Collectors.toList;
import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.FinancialMeansDeleted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class FinancialMeansListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Handles("progression.event.financial-means-deleted")
    public void deleteFinancialMeans(final JsonEnvelope event) {

        final FinancialMeansDeleted financialMeansDeleted = convertPayload(event);

        final List<CourtDocumentEntity> financialMeansCourtDocuments = courtDocumentRepository
                        .findByProsecutionCaseIdAndDefendantId(
                                        newArrayList(financialMeansDeleted.getCaseId()),
                                        newArrayList(financialMeansDeleted.getDefendantId()))
                        .stream().filter(CourtDocumentEntity::getContainsFinancialMeans)
                        .collect(toList());

        deleteFinancialMeansData(financialMeansCourtDocuments);
    }

    private FinancialMeansDeleted convertPayload(final JsonEnvelope event) {
        return jsonObjectConverter.convert(event.payloadAsJsonObject(),
                        FinancialMeansDeleted.class);
    }

    private void deleteFinancialMeansData(
                    final List<CourtDocumentEntity> financialMeansCourtDocuments) {
        deleteMaterialReferences(financialMeansCourtDocuments);
        deleteCourtDocuments(financialMeansCourtDocuments);
    }

    private void deleteMaterialReferences(
                    final List<CourtDocumentEntity> financialMeansCourtDocuments) {
        financialMeansCourtDocuments.stream().map(CourtDocumentEntity::getCourtDocumentId)
                        .map(courtDocumentMaterialRepository::findOptionalByCourtDocumentId)
                        .filter(Objects::nonNull).distinct()
                        .forEach(courtDocumentMaterialRepository::remove);
    }

    private void deleteCourtDocuments(
                    final List<CourtDocumentEntity> financialMeansCourtDocuments) {
        financialMeansCourtDocuments.stream().forEach(courtDocumentRepository::remove);
    }
}
