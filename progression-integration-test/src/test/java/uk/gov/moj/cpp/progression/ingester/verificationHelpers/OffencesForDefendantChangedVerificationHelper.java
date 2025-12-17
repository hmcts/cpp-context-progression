package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.core.IsEqual.equalTo;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class OffencesForDefendantChangedVerificationHelper extends BaseVerificationHelper {


    public static void verifyInitialOffence(final DocumentContext inputOffence, final JsonArray outputOffence) {
        with(outputOffence.toString())
                .assertThat("[0].offenceId", equalTo(((JsonString) inputOffence.read("$[0].id")).getString()));
    }

    public static void verifyUpdatedOffences(JsonObject inputProsecutionCaseJsonObject ,  JsonObject outputCase, final int inputPartyIndex, final int outputPartyIndex,
                                             final boolean refferedOffence,
                                             final String inputOffencesIdentifier) {

        BaseVerificationHelper verificationHelper = new BaseVerificationHelper();
        verificationHelper.validateOffences(inputProsecutionCaseJsonObject,outputCase, inputOffencesIdentifier);


    }
}
