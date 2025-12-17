package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.FinancialDataAdded;
import uk.gov.justice.core.courts.FinancialMeansDeleted;
import uk.gov.justice.core.courts.Material;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAggregateFinancialMeansTest {

    @InjectMocks
    private CaseAggregate caseAggregate;

    @BeforeEach
    public void setUp() {
        this.caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldApplyFinancialDataUpdatesForApplicationDocumentUpload() {

        UUID applicationId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        List<UUID> materialIds = new ArrayList<>();
        materialIds.add(materialId);

        FinancialDataAdded financialDataAdded = FinancialDataAdded.financialDataAdded()
                .withApplicationId(applicationId)
                .withMaterialIds(materialIds)
                .build();

        assertThat(caseAggregate.getApplicationFinancialDocs().size(), equalTo(0));

        this.caseAggregate.apply(financialDataAdded);

        assertThat(caseAggregate.getApplicationFinancialDocs().size(), equalTo(1));
    }

    @Test
    public void shouldApplyFinancialDataUpdatesForDefendantDocumentUpload() {

        UUID defendantId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        List<UUID> materialIds = new ArrayList<>();
        materialIds.add(materialId);

        FinancialDataAdded financialDataAdded = FinancialDataAdded.financialDataAdded()
                .withDefendantId(defendantId)
                .withMaterialIds(materialIds)
                .build();

        assertThat(caseAggregate.getApplicationFinancialDocs().size(), equalTo(0));
        assertThat(caseAggregate.getDefendantFinancialDocs().size(), equalTo(0));

        this.caseAggregate.apply(financialDataAdded);

        assertThat(caseAggregate.getApplicationFinancialDocs().size(), equalTo(0));
        assertThat(caseAggregate.getDefendantFinancialDocs().size(), equalTo(1));
    }

    @Test
    public void shouldApplyFinancialDataDelete() {

        UUID defendantId = UUID.randomUUID();
        updateAggregateWithDefendantFinancialData(defendantId);
        assertThat(caseAggregate.getDefendantFinancialDocs().size(), equalTo(1));

        FinancialMeansDeleted financialMeansDeleted = FinancialMeansDeleted.financialMeansDeleted().withDefendantId(defendantId).build();
        this.caseAggregate.apply(financialMeansDeleted);
        assertThat(caseAggregate.getDefendantFinancialDocs().size(), equalTo(0));
    }


    @Test
    public void shouldReturnFinancialDataAddedEventWhenDefendantUploadsMC100Form() {

        final CourtDocument courtDocument = getCourtDocumentWithDefendantDocumentCategory();
        final List<Object> eventStream = caseAggregate.addFinancialMeansData(courtDocument.getDocumentCategory().getDefendantDocument().getProsecutionCaseId(),
                courtDocument.getDocumentCategory().getDefendantDocument().getDefendants().get(0), null, courtDocument.getMaterials().get(0)).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(FinancialDataAdded.class)));
    }

    @Test
    public void shouldReturnFinancialMeansDeletedEvent() {

        UUID defendantId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a88");

        final List<Object> eventStream = caseAggregate.deleteFinancialMeansData(defendantId, UUID.randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(FinancialMeansDeleted.class)));
    }

    @Test
    public void shouldReturnFinancialDataAddedEventWhenApplicationUploadsMC100Form() {

        final CourtDocument courtDocument = getCourtDocumentWithApplicationDocumentCategory();
        final List<Object> eventStream = caseAggregate.addFinancialMeansData(courtDocument.getDocumentCategory().getApplicationDocument().getProsecutionCaseId(),
                null, courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId(), courtDocument.getMaterials().get(0)).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(FinancialDataAdded.class)));
    }

    private CourtDocument getCourtDocumentWithApplicationDocumentCategory() {
        UUID caseId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a77");
        UUID applicationId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a88");
        UUID materialId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a99");

        List<UUID> defendantlist = new ArrayList<>();
        defendantlist.add(applicationId);
        List<Material> materials = new ArrayList<>();
        Material material = Material.material().withId(materialId).withName("MC100").build();
        materials.add(material);


        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withContainsFinancialMeans(true)
                .withMaterials(materials)
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withApplicationDocument(ApplicationDocument.applicationDocument().withApplicationId(applicationId).withProsecutionCaseId(caseId).build())
                        .withDefendantDocument(DefendantDocument.defendantDocument().withDefendants(defendantlist).withProsecutionCaseId(caseId).build())
                        .build()
                ).build();

        return courtDocument;
    }

    private CourtDocument getCourtDocumentWithDefendantDocumentCategory() {
        UUID caseId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a77");
        UUID defendantId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a88");
        UUID materialId = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a99");

        List<UUID> defendantlist = new ArrayList<>();
        defendantlist.add(defendantId);
        List<Material> materials = new ArrayList<>();
        Material material = Material.material().withId(materialId).withName("MC100").build();
        materials.add(material);

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withContainsFinancialMeans(true)
                .withMaterials(materials)
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withDefendantDocument(DefendantDocument.defendantDocument().withDefendants(defendantlist).withProsecutionCaseId(caseId).build())
                        .build()
                ).build();

        return courtDocument;
    }


    private void updateAggregateWithDefendantFinancialData(final UUID defendantId) {

        UUID materialId = UUID.randomUUID();
        List<UUID> materialIds = new ArrayList<>();
        materialIds.add(materialId);
        FinancialDataAdded financialDataAdded = FinancialDataAdded.financialDataAdded()
                .withDefendantId(defendantId)
                .withMaterialIds(materialIds)
                .build();
        this.caseAggregate.apply(financialDataAdded);
    }
}
