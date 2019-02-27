package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.NowDocument;

import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
// GPE 6752 not dealling with applications
public class CourtDocumentTransformTest {

    private DocumentCategory.Builder documentCategory() {
        return DocumentCategory.documentCategory();
    }

    @Test
    public void testNowDocumentToIndex() {
        final NowDocument nowDocument = NowDocument.nowDocument()
                .withDefendantId(UUID.randomUUID())
                .withOrderHearingId(UUID.randomUUID())
                .withProsecutionCases(asList(UUID.randomUUID()))
                .build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeDescription("Notice")
                .withDocumentCategory(documentCategory()
                        .withNowDocument(nowDocument)
                        .build()
                ).build();
        final CourtDocumentIndex courtDocumentIndex = new CourtDocumentTransform().transform(courtDocument).build();
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
        final CaseDocument caseDocument = CaseDocument.caseDocument()
                .withProsecutionCaseId(UUID.randomUUID())
                .build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeDescription("Case stuff")
                .withDocumentCategory(documentCategory()
                        .withCaseDocument(caseDocument).build()
                ).build();
        final CourtDocumentIndex courtDocumentIndex = new CourtDocumentTransform().transform(courtDocument).build();
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
        final DefendantDocument defendantDocument = DefendantDocument.defendantDocument()
                .withDefendants(asList(UUID.randomUUID()))
                .withProsecutionCaseId(UUID.randomUUID())
                .build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeDescription("defendant info")
                .withDocumentCategory(documentCategory()
                        .withDefendantDocument(defendantDocument)
                        .build()
                ).build();
        final CourtDocumentIndex courtDocumentIndex = new CourtDocumentTransform().transform(courtDocument).build();
        assertThat(courtDocumentIndex.getCategory(), is(CourtDocumentTransform.DEFEENDANT_CATEGORY));
        assertThat(courtDocumentIndex.getType(), is(courtDocument.getDocumentTypeDescription()));
        assertThat(courtDocumentIndex.getDocument(), is(courtDocument));
        assertThat(courtDocumentIndex.getDefendantIds().size(), is(1));
        assertThat(courtDocumentIndex.getDefendantIds().get(0), is(defendantDocument.getDefendants().get(0)));
        assertThat(courtDocumentIndex.getCaseIds().size(), is(1));
        assertThat(courtDocumentIndex.getCaseIds().get(0), is(defendantDocument.getProsecutionCaseId()));
        assertThat(courtDocumentIndex.getHearingIds().size(), is(0));
    }

}
