package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// GPE 6752 not dealling with applications
public class CourtDocumentTransformTest {

    @Mock
    private CpsSendNotificationRepository cpsSendNotificationRepository;

    @InjectMocks
    private CourtDocumentTransform target;

    private DocumentCategory.Builder documentCategory() {
        return DocumentCategory.documentCategory();
    }

    @Test
    public void testNowDocumentToIndex() {
        final UUID courtDocumentId = UUID.randomUUID();
        final NowDocument nowDocument = NowDocument.nowDocument()
                .withDefendantId(UUID.randomUUID())
                .withOrderHearingId(UUID.randomUUID())
                .withProsecutionCases(asList(UUID.randomUUID()))
                .build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription("Notice")
                .withDocumentCategory(documentCategory()
                        .withNowDocument(nowDocument)
                        .build()
                ).build();
        when(cpsSendNotificationRepository.findBy(courtDocumentId)).thenReturn(createEntity(courtDocumentId));
        final CourtDocumentIndex courtDocumentIndex = target.transform(courtDocument,cpsSendNotificationRepository).build();
        assertThat(courtDocumentIndex.getCategory(), is(CourtDocumentTransform.NOW_CATEGORY));
        assertThat(courtDocumentIndex.getType(), is(courtDocument.getDocumentTypeDescription()));
        assertThat(courtDocumentIndex.getDocument(), is(courtDocument));
        assertThat(courtDocumentIndex.getDefendantIds().size(), is(1));
        assertThat(courtDocumentIndex.getDefendantIds().get(0), is(nowDocument.getDefendantId()));
        assertThat(courtDocumentIndex.getCaseIds().size(), is(1));
        assertThat(courtDocumentIndex.getCaseIds().get(0), is(nowDocument.getProsecutionCases().get(0)));
        assertThat(courtDocumentIndex.getHearingIds().size(), is(1));
        assertThat(courtDocumentIndex.getHearingIds().get(0), is(nowDocument.getOrderHearingId()));
    }

    @Test
    public void testCaseDocumentToIndex() {
        final UUID courtDocumentId = UUID.randomUUID();
        final CaseDocument caseDocument = CaseDocument.caseDocument()
                .withProsecutionCaseId(UUID.randomUUID())
                .build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription("Case stuff")
                .withDocumentCategory(documentCategory()
                        .withCaseDocument(caseDocument).build()
                ).build();
        when(cpsSendNotificationRepository.findBy(courtDocumentId)).thenReturn(createEntity(courtDocumentId));
        final CourtDocumentIndex courtDocumentIndex = target.transform(courtDocument, cpsSendNotificationRepository).build();
        assertThat(courtDocumentIndex.getCategory(), is(CourtDocumentTransform.CASE_CATEGORY));
        assertThat(courtDocumentIndex.getType(), is(courtDocument.getDocumentTypeDescription()));
        assertThat(courtDocumentIndex.getDocument(), is(courtDocument));
        assertThat(courtDocumentIndex.getDefendantIds().size(), is(0));
        assertThat(courtDocumentIndex.getCaseIds().size(), is(1));
        assertThat(courtDocumentIndex.getCaseIds().get(0), is(caseDocument.getProsecutionCaseId()));
        assertThat(courtDocumentIndex.getHearingIds().size(), is(0));
    }

    @Test
    public void testDefendantDocumentToIndex() {
        final UUID courtDocumentId = UUID.randomUUID();

        final DefendantDocument defendantDocument = DefendantDocument.defendantDocument()
                .withDefendants(asList(UUID.randomUUID()))
                .withProsecutionCaseId(UUID.randomUUID())
                .build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription("defendant info")
                .withDocumentCategory(documentCategory()
                        .withDefendantDocument(defendantDocument)
                        .build()
                ).build();
        when(cpsSendNotificationRepository.findBy(courtDocumentId)).thenReturn(createEntity(courtDocumentId));
        final CourtDocumentIndex courtDocumentIndex = target.transform(courtDocument,cpsSendNotificationRepository).build();
        assertThat(courtDocumentIndex.getCategory(), is(CourtDocumentTransform.DEFEENDANT_CATEGORY));
        assertThat(courtDocumentIndex.getType(), is(courtDocument.getDocumentTypeDescription()));
        assertThat(courtDocumentIndex.getDocument(), is(courtDocument));
        assertThat(courtDocumentIndex.getDefendantIds().size(), is(1));
        assertThat(courtDocumentIndex.getDefendantIds().get(0), is(defendantDocument.getDefendants().get(0)));
        assertThat(courtDocumentIndex.getCaseIds().size(), is(1));
        assertThat(courtDocumentIndex.getCaseIds().get(0), is(defendantDocument.getProsecutionCaseId()));
        assertThat(courtDocumentIndex.getHearingIds().size(), is(0));
    }

    private CpsSendNotificationEntity createEntity(final UUID courtDocumentId){
        final CpsSendNotificationEntity cpsSendNotificationEntity = new CpsSendNotificationEntity();
        cpsSendNotificationEntity.setSendToCps(true);
        cpsSendNotificationEntity.setCourtDocumentId(courtDocumentId);

        return cpsSendNotificationEntity;
    }

}
