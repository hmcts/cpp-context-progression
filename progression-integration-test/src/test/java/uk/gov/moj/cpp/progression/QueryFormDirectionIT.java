package uk.gov.moj.cpp.progression;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import java.io.IOException;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.DMConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.DMConstants.DIRECTION_REF_DATA_ID;
import static uk.gov.moj.cpp.progression.helper.DirectionVerificationHelper.verifyTransformedQueryFormDirection;
import static uk.gov.moj.cpp.progression.helper.DirectionVerificationHelper.verifyTransformedQueryFormDirectionWithoutCategories;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialStructuredFormQuery;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataDirectionStub.stubGetReferenceDataAllDirection;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataDirectionStub.stubGetReferenceDataDirectionManagementType;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

public class QueryFormDirectionIT extends AbstractIT {

    public static final String FORM_ID = "d0dff4cf-c833-4758-a54a-d18ad004095d";
    public static final String PET = "PET";
    public static final String PTPH = "PTPH";
    public static final String CATEGORIES = "pet_witness,pet_part_5";

    static final JsonObject jsonObject = JsonObjects.createObjectBuilder()
            .add("data", JsonObjects.createObjectBuilder()
                    .add("prosecution", JsonObjects.createObjectBuilder()
                            .add("witnesses", JsonObjects.createArrayBuilder()
                                    .add(JsonObjects.createObjectBuilder()
                                            .add("id", "84ec2958-8ab2-4b90-b32f-f3d5534d5ec9")
                                            .add("firstName", "Firstname")
                                            .add("lastName", "Lastname")
                                            .add("intermediaryFirstName", "NameofintermediaryKnownatPTPH")
                                            .add("intermediaryLastName", "NameofintermediaryKnownatPTPH")
                                            .add("collarNumber", "Theofficercollar/shouldernumber")
                                            .add("rank", "Theofficerrank")
                                            .add("relevantDisputedIssue", "Relevantdisputedissue")
                                            .add("details", JsonObjects.createArrayBuilder()
                                                    .add("INTERMEDIARY")
                                                    .add("POLICE_OFFICER")
                                            )
                                    )
                            )
                    )
                    .add("defence", JsonObjects.createObjectBuilder())
            )
            .add("lastUpdated", "2021-01-13T00:00Z[UTC]").build();

    private static final String UPDATED_FORM_DATA = createObjectBuilder()
            .add("name", "updated name")
            .add("offence", "burglary")
            .add("form_type", "PET")
            .add("prosecution", "prosecution")
            .add("caseParticipant","Witness")
            .add("petId", "733d43c3-4e04-464d-a8a7-c96fcc22bc38")
            .add("formId", "d0dff4cf-c833-4758-a54a-d18ad004095d")
            .add("data", jsonObject)
            .add("lastUpdated","2021-01-13T00:00Z[UTC]").build().toString();

    @BeforeEach
    public void setUp() throws JSONException, IOException {
        stubGetReferenceDataDirectionManagementType();
        stubGetReferenceDataAllDirection();
        String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(CASE_ID, defendantId);
        pollProsecutionCasesProgressionFor(CASE_ID, getProsecutionCaseMatchers(CASE_ID, defendantId));
        stubMaterialStructuredFormQuery(UPDATED_FORM_DATA);
    }

    @Test
    public void shouldQueryFormDirectionPETForm()  {
        verifyTransformedQueryFormDirection(CASE_ID, FORM_ID, CATEGORIES, PET, DIRECTION_REF_DATA_ID);
    }

    @Test
    public void shouldQueryFormDirectionPETFormWithoutCategories() {
        verifyTransformedQueryFormDirectionWithoutCategories(CASE_ID, FORM_ID, PET, DIRECTION_REF_DATA_ID);
    }

    @Test
    public void shouldQueryFormDirectionPTPHForm() {
        verifyTransformedQueryFormDirection(CASE_ID, FORM_ID, CATEGORIES, PTPH, DIRECTION_REF_DATA_ID);
    }

}
