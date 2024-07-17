package uk.gov.moj.cpp.progression.transformer;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildCourtDocument;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferredCourtDocumentTransformerTest {

    public static final String CASE_DOCUMENT = "CaseDocument";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private RefDataService referenceDataService;
    @InjectMocks
    private ReferredCourtDocumentTransformer referredCourtDocumentTransformer;

    @Mock
    private Requester requester;

    @Test
    public void testTransform() {
        // Setup
        final UUID documentTypeId = randomUUID();
        final ReferredCourtDocument referredCourtDocument = buildCourtDocument(documentTypeId);
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("section", CASE_DOCUMENT)
                .add("seqNum", 10)
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer").build()).build())
                                .add("readUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build())
                                .add("downloadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build()).build()
                )
                .build();

        when(referenceDataService.getDocumentTypeAccessData(documentTypeId, jsonEnvelope, requester))
                .thenReturn(Optional.of(jsonObject));

        // Run the test
        final CourtDocument result = referredCourtDocumentTransformer.transform
                (referredCourtDocument, jsonEnvelope);

        // Verify the results
        assertThat(documentTypeId, is(result.getDocumentTypeId()));
        assertThat(CASE_DOCUMENT, is(result.getDocumentTypeDescription()));
        assertThat(result.getContainsFinancialMeans(), is(true));
        assertThat(10, is(result.getSeqNum()));
        assertThat(result.getDocumentTypeRBAC(), notNullValue());
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }

    @Test
    public void shouldThrowException() {
        expectedException.expect(ReferenceDataNotFoundException.class);
        // Setup
        final UUID documentTypeId = randomUUID();
        final ReferredCourtDocument referredCourtDocument = buildCourtDocument(documentTypeId);
        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(referenceDataService.getDocumentTypeAccessData(documentTypeId, jsonEnvelope, requester))
                .thenThrow(new ReferenceDataNotFoundException("", ""));

        // Run the test
        referredCourtDocumentTransformer.transform
                (referredCourtDocument, jsonEnvelope);

        verifyNoMoreInteractions(referenceDataService);
    }


}
