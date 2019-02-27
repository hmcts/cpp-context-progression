package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutionCaseAtAGlanceFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class CourtExtractIT {

    private String caseId;
    private String defendantId;
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";
    private static final String CERTIFICATE_OF_CONVICTION = "CertificateOfConviction";


    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        createMockEndpoints();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
    }


    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCrownCourtExtract() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String prosecutionCasesResponse = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(prosecutionCasesResponse);

        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        final String prosecutionCaseAtAGlanceResponse = getProsecutionCaseAtAGlanceFor(caseId);
        final JsonObject prosecutionCaseAtAGlanceJason = getJsonObject(prosecutionCaseAtAGlanceResponse);
        assertEquals(caseId, prosecutionCaseAtAGlanceJason.getString("id"));

        final String hearingId = prosecutionCaseAtAGlanceJason.getJsonArray("hearings").getJsonObject(0).getString("id");

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, hearingId, CROWN_COURT_EXTRACT);
        // then
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCertificateOfConviction() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String prosecutionCasesResponse = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(prosecutionCasesResponse);

        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        final String prosecutionCaseAtAGlanceResponse = getProsecutionCaseAtAGlanceFor(caseId);
        final JsonObject prosecutionCaseAtAGlanceJason = getJsonObject(prosecutionCaseAtAGlanceResponse);
        assertEquals(caseId, prosecutionCaseAtAGlanceJason.getString("id"));

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, "", CERTIFICATE_OF_CONVICTION);
        // then
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

}

