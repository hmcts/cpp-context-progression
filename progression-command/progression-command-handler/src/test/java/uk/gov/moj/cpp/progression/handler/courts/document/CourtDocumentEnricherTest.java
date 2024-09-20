package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentEnricherTest {

    @Mock
    private DocumentTypeRBACFactory documentTypeRBACFactory;

    @Mock
    private EnrichedMaterialsProvider enrichedMaterialsProvider;

    @InjectMocks
    private CourtDocumentEnricher courtDocumentEnricher;

    @Test
    public void shouldEnrichCourtDocumentWithMaterialUserGroups() throws Exception {

        final LocalDate amendmentDate = new UtcClock().now().toLocalDate();
        final boolean containsFinancialMeans = true;
        final UUID courtDocumentId = randomUUID();
        final DocumentCategory documentCategory = mock(DocumentCategory.class);
        final String documentTypeDescription = "document type description";
        final UUID documentTypeId = randomUUID();
        final boolean isRemoved = false;
        final List<Material> materials = singletonList(mock(Material.class));
        final String mimeType = "mime/type";
        final String name = "name";
        final int seqNum = 23;

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withAmendmentDate(amendmentDate)
                .withContainsFinancialMeans(containsFinancialMeans)
                .withCourtDocumentId(courtDocumentId)
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(documentTypeDescription)
                .withDocumentTypeId(documentTypeId)
                .withDocumentTypeRBAC(mock(DocumentTypeRBAC.class))
                .withIsRemoved(isRemoved)
                .withMaterials(materials)
                .withMimeType(mimeType)
                .withName(name)
                .withSendToCps(false)
                .withSeqNum(seqNum)
                .build();


        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);

        final DocumentTypeRBAC documentTypeRBAC = mock(DocumentTypeRBAC.class);
        final List<Material> enrichedMaterials = singletonList(mock(Material.class));

        when(enrichedMaterialsProvider.getEnrichedMaterials(courtDocument, documentTypeAccess)).thenReturn(enrichedMaterials);
        when(documentTypeRBACFactory.createFromMaterialUserGroups(documentTypeAccess)).thenReturn(documentTypeRBAC);

        when(documentTypeAccess.getSeqNum()).thenReturn(45);

        final CourtDocument enrichedCourtDocument = courtDocumentEnricher.enrichWithMaterialUserGroups(
                courtDocument,
                documentTypeAccess);

        assertThat(enrichedCourtDocument.getAmendmentDate(), is(nullValue()));
        assertThat(enrichedCourtDocument.getContainsFinancialMeans(), is(containsFinancialMeans));
        assertThat(enrichedCourtDocument.getCourtDocumentId(), is(courtDocumentId));
        assertThat(enrichedCourtDocument.getDocumentCategory(), is(documentCategory));
        assertThat(enrichedCourtDocument.getDocumentTypeDescription(), is(documentTypeDescription));
        assertThat(enrichedCourtDocument.getDocumentTypeRBAC(), is(documentTypeRBAC));
        assertThat(enrichedCourtDocument.getDocumentTypeId(), is(documentTypeId));
        assertThat(enrichedCourtDocument.getIsRemoved(), is(nullValue()));
        assertThat(enrichedCourtDocument.getMaterials(), is(enrichedMaterials));
        assertThat(enrichedCourtDocument.getMimeType(), is(mimeType));
        assertThat(enrichedCourtDocument.getName(), is(name));
        assertThat(enrichedCourtDocument.getSeqNum(), is(45));
    }

    @Test
    public void shouldGiveNewSequenceNumberOfZeroIfNoneFound() throws Exception {

        final LocalDate amendmentDate = new UtcClock().now().toLocalDate();
        final boolean containsFinancialMeans = true;
        final UUID courtDocumentId = randomUUID();
        final DocumentCategory documentCategory = mock(DocumentCategory.class);
        final String documentTypeDescription = "document type description";
        final UUID documentTypeId = randomUUID();
        final boolean isRemoved = false;
        final List<Material> materials = singletonList(mock(Material.class));
        final String mimeType = "mime/type";
        final String name = "name";
        final int seqNum = 23;

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withAmendmentDate(amendmentDate)
                .withContainsFinancialMeans(containsFinancialMeans)
                .withCourtDocumentId(courtDocumentId)
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(documentTypeDescription)
                .withDocumentTypeId(documentTypeId)
                .withDocumentTypeRBAC(mock(DocumentTypeRBAC.class))
                .withIsRemoved(isRemoved)
                .withMaterials(materials)
                .withMimeType(mimeType)
                .withName(name)
                .withSendToCps(false)
                .withSeqNum(seqNum)
                .build();


        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);

        final DocumentTypeRBAC documentTypeRBAC = mock(DocumentTypeRBAC.class);
        final List<Material> enrichedMaterials = singletonList(mock(Material.class));

        when(enrichedMaterialsProvider.getEnrichedMaterials(courtDocument, documentTypeAccess)).thenReturn(enrichedMaterials);
        when(documentTypeRBACFactory.createFromMaterialUserGroups(documentTypeAccess)).thenReturn(documentTypeRBAC);

        when(documentTypeAccess.getSeqNum()).thenReturn(null);

        final CourtDocument enrichedCourtDocument = courtDocumentEnricher.enrichWithMaterialUserGroups(
                courtDocument,
                documentTypeAccess);

        assertThat(enrichedCourtDocument.getAmendmentDate(), is(nullValue()));
        assertThat(enrichedCourtDocument.getContainsFinancialMeans(), is(containsFinancialMeans));
        assertThat(enrichedCourtDocument.getCourtDocumentId(), is(courtDocumentId));
        assertThat(enrichedCourtDocument.getDocumentCategory(), is(documentCategory));
        assertThat(enrichedCourtDocument.getDocumentTypeDescription(), is(documentTypeDescription));
        assertThat(enrichedCourtDocument.getDocumentTypeRBAC(), is(documentTypeRBAC));
        assertThat(enrichedCourtDocument.getDocumentTypeId(), is(documentTypeId));
        assertThat(enrichedCourtDocument.getIsRemoved(), is(nullValue()));
        assertThat(enrichedCourtDocument.getMaterials(), is(enrichedMaterials));
        assertThat(enrichedCourtDocument.getMimeType(), is(mimeType));
        assertThat(enrichedCourtDocument.getName(), is(name));
        assertThat(enrichedCourtDocument.getSeqNum(), is(0));

    }
}
