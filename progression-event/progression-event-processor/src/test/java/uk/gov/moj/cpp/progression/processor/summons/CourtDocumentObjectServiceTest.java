package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentObjectServiceTest {

    private static final UUID CASE_SUMMONS_DOCUMENT_TYPE_ID = UUID.fromString("460f7ec0-c002-11e8-a355-529269fb1459");
    private static final UUID APPLICATIONS_DOCUMENT_TYPE_ID = UUID.fromString("460fa7ce-c002-11e8-a355-529269fb1459");

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final String DOCUMENT_SECTION_NAME = "random section";
    private static final String SUMMONS = "Summons";

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private CourtDocumentObjectService courtDocumentObjectService;

    @Test
    public void buildCaseSummonsCourtDocument() {
        when(referenceDataService.getDocumentTypeAccessData(CASE_SUMMONS_DOCUMENT_TYPE_ID, jsonEnvelope, requester)).thenReturn(getDocumentTypeData());

        final CourtDocument courtDocument = courtDocumentObjectService.buildCaseSummonsCourtDocument(CASE_ID, DEFENDANT_ID, MATERIAL_ID, jsonEnvelope);

        verify(referenceDataService).getDocumentTypeAccessData(CASE_SUMMONS_DOCUMENT_TYPE_ID, jsonEnvelope, requester);

        assertThat(courtDocument, notNullValue());
        assertThat(courtDocument.getDocumentCategory().getDefendantDocument().getProsecutionCaseId(), is(CASE_ID));
        assertThat(courtDocument.getDocumentCategory().getDefendantDocument().getDefendants(), hasItem(DEFENDANT_ID));

        assertThat(courtDocument.getCourtDocumentId(), notNullValue());
        assertThat(courtDocument.getDocumentTypeId(), is(CASE_SUMMONS_DOCUMENT_TYPE_ID));
        assertThat(courtDocument.getDocumentTypeDescription(), is(DOCUMENT_SECTION_NAME));
        assertThat(courtDocument.getName(), is(SUMMONS));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));

        final Material firstMaterial = courtDocument.getMaterials().get(0);
        assertThat(firstMaterial.getId(), is(MATERIAL_ID));
        assertThat(firstMaterial.getGenerationStatus(), nullValue());
        assertThat(firstMaterial.getUploadDateTime(), notNullValue());
        assertThat(firstMaterial.getName(), is(SUMMONS));
    }

    @Test
    public void buildApplicationSummonsCourtDocument() {
        when(referenceDataService.getDocumentTypeAccessData(APPLICATIONS_DOCUMENT_TYPE_ID, jsonEnvelope, requester)).thenReturn(getDocumentTypeData());

        final CourtDocument courtDocument = courtDocumentObjectService.buildApplicationSummonsCourtDocument(APPLICATION_ID, MATERIAL_ID, jsonEnvelope);

        verify(referenceDataService).getDocumentTypeAccessData(APPLICATIONS_DOCUMENT_TYPE_ID, jsonEnvelope, requester);

        assertThat(courtDocument, notNullValue());
        assertThat(courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId(), is(APPLICATION_ID));
        assertThat(courtDocument.getDocumentCategory().getApplicationDocument().getProsecutionCaseId(), nullValue());

        assertThat(courtDocument.getCourtDocumentId(), notNullValue());
        assertThat(courtDocument.getDocumentTypeId(), is(APPLICATIONS_DOCUMENT_TYPE_ID));
        assertThat(courtDocument.getDocumentTypeDescription(), is(DOCUMENT_SECTION_NAME));
        assertThat(courtDocument.getName(), is(SUMMONS));
        assertThat(courtDocument.getMimeType(), is("application/pdf"));

        final Material firstMaterial = courtDocument.getMaterials().get(0);
        assertThat(firstMaterial.getId(), is(MATERIAL_ID));
        assertThat(firstMaterial.getGenerationStatus(), nullValue());
        assertThat(firstMaterial.getUploadDateTime(), notNullValue());
        assertThat(firstMaterial.getName(), is(SUMMONS));
    }

    private Optional<JsonObject> getDocumentTypeData() {
        return of(createObjectBuilder().add("section", DOCUMENT_SECTION_NAME).build());
    }
}