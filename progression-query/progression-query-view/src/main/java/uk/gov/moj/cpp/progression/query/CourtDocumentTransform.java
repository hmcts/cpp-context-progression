package uk.gov.moj.cpp.progression.query;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;

import java.util.Arrays;

@SuppressWarnings({"squid:S2789", "squid:S3655"})
public class CourtDocumentTransform {

    public static final String CASE_CATEGORY = "Case level";
    public static final String DEFEENDANT_CATEGORY = "Defendant level";
    public static final String APPLICATION_CATEGORY = "Applications";
    public static final String NOW_CATEGORY = "NOW documents";

    public CourtDocumentIndex.Builder transform(final CourtDocument courtDocument, final CpsSendNotificationRepository cpsSendNotificationRepository) {
        final CourtDocumentIndex.Builder indexBuilder = new CourtDocumentIndex.Builder();
        indexBuilder.withDocument(courtDocument);
        final DocumentCategory category = courtDocument.getDocumentCategory();
        if (nonNull(category.getNowDocument())) {
            map(category.getNowDocument(), indexBuilder);
        } else if (nonNull(category.getCaseDocument())) {
            map(category.getCaseDocument(), indexBuilder);
        } else if (nonNull(category.getDefendantDocument())) {
            map(category.getDefendantDocument(), indexBuilder);
        } else if (nonNull(category.getApplicationDocument())) {
            map(indexBuilder);
        }
        indexBuilder.withType(nonNull(courtDocument.getDocumentTypeDescription())? courtDocument.getDocumentTypeDescription(): "unknown document type");

        final CpsSendNotificationEntity cpsSendNotificationEntity = cpsSendNotificationRepository.findBy(courtDocument.getCourtDocumentId());
        if (nonNull(cpsSendNotificationEntity)){
            indexBuilder.withSendToCps(cpsSendNotificationEntity.getSendToCps());
        }

        return indexBuilder;
    }

    private void map(final NowDocument document, CourtDocumentIndex.Builder index) {
        index.withCaseIds(document.getProsecutionCases());
        index.withDefendantIds(Arrays.asList(document.getDefendantId()));
        index.withHearingIds(Arrays.asList(document.getOrderHearingId()));
        index.withCategory(NOW_CATEGORY);
    }

    private void map(final CaseDocument document, CourtDocumentIndex.Builder index) {
        index.withCaseIds(Arrays.asList(document.getProsecutionCaseId()));
        index.withDefendantIds(emptyList());
        index.withHearingIds(emptyList());
        index.withCategory(CASE_CATEGORY);
    }

    private void map(final DefendantDocument document, CourtDocumentIndex.Builder index) {
        index.withCaseIds(Arrays.asList(document.getProsecutionCaseId()));
        index.withDefendantIds(document.getDefendants());
        index.withHearingIds(emptyList());
        index.withCategory(DEFEENDANT_CATEGORY);
    }

    private void map(CourtDocumentIndex.Builder index) {
        // GPE-6752 cant link applications to anything ?!?!!?
        index.withCaseIds(emptyList());
        index.withDefendantIds(emptyList());
        index.withHearingIds(emptyList());
        index.withCategory(APPLICATION_CATEGORY);
    }

}
