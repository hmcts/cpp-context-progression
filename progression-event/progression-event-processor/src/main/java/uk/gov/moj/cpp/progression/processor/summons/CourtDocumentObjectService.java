package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.ApplicationDocument.applicationDocument;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;
import static uk.gov.justice.core.courts.DefendantDocument.defendantDocument;
import static uk.gov.justice.core.courts.DocumentCategory.documentCategory;
import static uk.gov.justice.core.courts.Material.material;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtDocumentObjectService {

    private static final UUID CASE_SUMMONS_DOCUMENT_TYPE_ID = UUID.fromString("460f7ec0-c002-11e8-a355-529269fb1459");
    private static final UUID APPLICATIONS_DOCUMENT_TYPE_ID = UUID.fromString("460fa7ce-c002-11e8-a355-529269fb1459");
    private static final String SUMMONS = "Summons";

    @Inject
    private RefDataService referenceDataService;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    public CourtDocument buildCaseSummonsCourtDocument(final UUID caseId, final UUID defendantId, final UUID materialId, final JsonEnvelope jsonEnvelope) {
        final List<UUID> defendants = newArrayList();
        defendants.add(defendantId);
        final DefendantDocument defendantDocument = defendantDocument()
                .withDefendants(defendants)
                .withProsecutionCaseId(caseId)
                .build();

        final DocumentCategory documentCategory = documentCategory()
                .withDefendantDocument(defendantDocument)
                .build();
        final JsonObject documentTypeData = referenceDataService.getDocumentTypeAccessData(CASE_SUMMONS_DOCUMENT_TYPE_ID, jsonEnvelope, requester)
                .orElseThrow(() -> new RuntimeException("Unable to retrieve document type details for '" + CASE_SUMMONS_DOCUMENT_TYPE_ID + "'"));
        return courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentTypeId(CASE_SUMMONS_DOCUMENT_TYPE_ID)
                .withDocumentTypeDescription(documentTypeData.getString("section"))
                .withMaterials(singletonList(material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withName(SUMMONS)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory)
                .withName(SUMMONS)
                .withMimeType("application/pdf")
                .build();
    }

    public CourtDocument buildApplicationSummonsCourtDocument(final UUID applicationId, final UUID materialId, final JsonEnvelope jsonEnvelope) {
        final ApplicationDocument applicationDocument = applicationDocument()
                .withApplicationId(applicationId)
                .build();

        final DocumentCategory documentCategory = documentCategory()
                .withApplicationDocument(applicationDocument)
                .build();

        final JsonObject documentTypeData = referenceDataService.getDocumentTypeAccessData(APPLICATIONS_DOCUMENT_TYPE_ID, jsonEnvelope, requester)
                .orElseThrow(() -> new RuntimeException("Unable to retrieve document type details for '" + APPLICATIONS_DOCUMENT_TYPE_ID + "'"));

        return courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentTypeId(APPLICATIONS_DOCUMENT_TYPE_ID)
                .withDocumentTypeDescription(documentTypeData.getString("section"))
                .withMaterials(Collections.singletonList(Material.material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withName(SUMMONS)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory)
                .withName(SUMMONS)
                .withMimeType("application/pdf")
                .build();
    }
}
