package uk.gov.moj.cpp.progression.helper;

import static com.google.common.io.Resources.getResource;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import com.google.common.io.Resources;
import io.restassured.response.Response;

public class CourtApplicationsHelper {

    public static String getStandaloneCourtApplicationWithRespondentsJsonBody(final String fileName, final CourtApplicationsHelper.CourtApplicationRandomValues randomValues) throws IOException {

        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replaceAll("RANDOM_APPLICATION_ID", randomValues.RANDOM_APPLICATION_ID)
                .replaceAll("RANDOM_PARENT_APPLICATION_ID", randomValues.RANDOM_PARENT_APPLICATION_ID)
                .replaceAll("RANDOM_REFERENCE", PreAndPostConditionHelper.generateUrn())
                .replaceAll("RANDOM_APPLICANT_FIRSTNAME", randomValues.APPLICANT_FIRSTNAME)
                .replaceAll("RANDOM_APPLICANT_MIDDLENAME", randomValues.APPLICANT_MIDDLENAME)
                .replaceAll("RANDOM_APPLICANT_LASTNAME", randomValues.APPLICANT_LASTNAME)
                .replaceAll("RANDOM_RESPONDENT_FIRSTNAME", randomValues.RESPONDENT_FIRSTNAME)
                .replaceAll("RANDOM_RESPONDENT_MIDDLENAME", randomValues.RESPONDENT_MIDDLENAME)
                .replaceAll("RANDOM_RESPONDENT_LASTNAME", randomValues.RESPONDENT_LASTNAME)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_NAME", randomValues.RESPONDENT_ORGANISATION_NAME)

                .replaceAll("RANDOM_INDIVIDUAL_FIRST_NAME", randomValues.RANDOM_INDIVIDUAL_FIRST_NAME)
                .replaceAll("RANDOM_INDIVIDUAL_LAST_NAME", randomValues.RANDOM_INDIVIDUAL_LAST_NAME)
                .replaceAll("RANDOM_INDIVIDUAL_ADDRESS", randomValues.RANDOM_INDIVIDUAL_ADDRESS)
                .replaceAll("RANDOM_INDIVIDUAL_ORG_NAME", randomValues.RANDOM_INDIVIDUAL_ORG_NAME)
                .replaceAll("RANDOM_INDIVIDUAL_ORG_ADDRESS", randomValues.RANDOM_INDIVIDUAL_ORG_ADDRESS)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_NAME", randomValues.RANDOM_RESPONDENT_ORGANISATION_NAME)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS1", randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS1)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS2", randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS2)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS3", randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS3)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS4", randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS4)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS5", randomValues.RANDOM_RESPONDENT_ORGANISATION_ADDRESS5)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_POSTCODE", randomValues.RANDOM_RESPONDENT_ORGANISATION_POSTCODE)
                .replaceAll("RANDOM_RESPONDENT_REPRESENTATIVE_FIRST_NAME", randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_FIRST_NAME)
                .replaceAll("RANDOM_RESPONDENT_REPRESENTATIVE_LAST_NAME", randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_LAST_NAME)
                .replaceAll("RANDOM_RESPONDENT_REPRESENTATIVE_POSITION", randomValues.RANDOM_RESPONDENT_REPRESENTATIVE_POSITION);
    }

    public static Response addStandaloneCourtApplicationWithRespondents(final String fileName, CourtApplicationsHelper.CourtApplicationRandomValues randomValues) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                getStandaloneCourtApplicationWithRespondentsJsonBody(fileName, randomValues));
    }

    public static class CourtApplicationRandomValues {

        final public String APPLICANT_FIRSTNAME = new StringGenerator().next();
        final public String APPLICANT_MIDDLENAME = new StringGenerator().next();
        final public String APPLICANT_LASTNAME = new StringGenerator().next();

        final public String RESPONDENT_FIRSTNAME = new StringGenerator().next();
        final public String RESPONDENT_MIDDLENAME = new StringGenerator().next();
        final public String RESPONDENT_LASTNAME = new StringGenerator().next();
        final public String RESPONDENT_ORGANISATION_NAME = new StringGenerator().next();

        final public String RANDOM_INDIVIDUAL_FIRST_NAME = new StringGenerator().next();
        final public String RANDOM_INDIVIDUAL_LAST_NAME = new StringGenerator().next();
        final public String RANDOM_INDIVIDUAL_ADDRESS = new StringGenerator().next();
        final public String RANDOM_INDIVIDUAL_ORG_NAME = new StringGenerator().next();
        final public String RANDOM_INDIVIDUAL_ORG_ADDRESS = new StringGenerator().next();

        final public String RANDOM_RESPONDENT_ORGANISATION_NAME = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_ORGANISATION_ADDRESS1 = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_ORGANISATION_ADDRESS2 = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_ORGANISATION_ADDRESS3 = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_ORGANISATION_ADDRESS4 = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_ORGANISATION_ADDRESS5 = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_ORGANISATION_POSTCODE = new StringGenerator().next();

        final public String RANDOM_RESPONDENT_REPRESENTATIVE_FIRST_NAME = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_REPRESENTATIVE_LAST_NAME = new StringGenerator().next();
        final public String RANDOM_RESPONDENT_REPRESENTATIVE_POSITION = new StringGenerator().next();

        final public String RANDOM_INDIVIDUAL_ID = UUID.randomUUID().toString();
        final public String RANDOM_ORG_ID = UUID.randomUUID().toString();
        final public String RANDOM_APPLICATION_ID = UUID.randomUUID().toString();
        final public String RANDOM_PARENT_APPLICATION_ID = UUID.randomUUID().toString();

    }

}
