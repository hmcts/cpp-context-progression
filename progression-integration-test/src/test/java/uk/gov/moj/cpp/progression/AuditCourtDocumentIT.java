package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class AuditCourtDocumentIT extends AbstractIT {


    private String caseId;
    private String courtDocumentId;
    private String materialId;
    private String defendantId;
    public static final String USER_GROUP_NOT_PRESENT_RBAC = UUID.randomUUID().toString();
    private static final String PRIVATE_COURT_DOCUMENT_AUDIT_EVENT = "progression.event.court-document-audit";

    private static final MessageConsumer messageConsumerClientPrivateForCourtDocumentAudit = privateEvents.createPrivateConsumer(PRIVATE_COURT_DOCUMENT_AUDIT_EVENT);




    @BeforeClass
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
    }
    @Before
    public void setup() throws IOException {
        caseId = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        materialId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));
    }

    @Test
    public void shouldAuditCourtDocumentForbidden() throws IOException {
        verifyAddCourtDocument();
        final String body = prepareAuditCourtDocumentPayload("View");
        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/courtdocuments/" + courtDocumentId + "/materials/" + materialId + "/audit"),
                "application/vnd.progression.audit-court-document+json",
                body, USER_GROUP_NOT_PRESENT_RBAC);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_FORBIDDEN));
    }

    @Test
    public void shouldAuditCourtDocument() throws IOException {
         verifyAddCourtDocument();
        final String body = prepareAuditCourtDocumentPayload("View");
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocuments/" + courtDocumentId + "/materials/" + materialId + "/audit"),
                "application/vnd.progression.audit-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentAudit);

    }

    private static void verifyInMessagingQueue(final MessageConsumer messageConsumer) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumer);
        assertTrue(message.isPresent());
    }


    private void verifyAddCourtDocument() throws IOException {
        //Given
        final String body = prepareAddCourtDocumentPayload();
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + courtDocumentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        final String actualDocument = getCourtDocumentFor(courtDocumentId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)))
        );

        final String expectedPayload = getPayload("expected/expected.progression.add-court-document-material.json")
                .replace("COURT-DOCUMENT-ID", courtDocumentId.toString())
                .replace("DEFENDENT-ID", defendantId.toString())
                .replace("MATERIAL_ID", materialId.toString())
                .replace("CASE-ID", caseId.toString());

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document-material.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", courtDocumentId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_MATERIAL_ID%", materialId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        return body;
    }


    private String prepareAuditCourtDocumentPayload(String action) {
        String body = getPayload("progression.audit-court-document.json");
        body = body.replaceAll("%RANDOM_USER_ACTION%", action);
        return body;
    }




    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

}
