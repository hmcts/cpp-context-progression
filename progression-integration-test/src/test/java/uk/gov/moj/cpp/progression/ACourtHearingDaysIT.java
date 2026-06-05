package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import static com.google.common.io.Resources.getResource;
import static java.util.UUID.randomUUID;
import org.json.JSONException;
import org.json.JSONObject;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class ACourtHearingDaysIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final RestClient restClient = new RestClient();

    public static io.restassured.response.Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String reportingRestrictionOrderedDate) throws IOException, JSONException {
        return addProsecutionCaseToCrownCourt(caseId, defendantId, generateUrn(), reportingRestrictionOrderedDate);
    }

    public static io.restassured.response.Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String caseUrn, final String reportingRestrictionOrderedDate) throws IOException, JSONException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, reportingRestrictionOrderedDate));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    private static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                         final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                         final String caseUrn, final String reportingRestrictionOrderedDate) throws IOException {
        return createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, reportingRestrictionOrderedDate, "progression.command.prosecution-case-refer-to-court.json");
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String reportingRestrictionOrderedDate, final String filePath) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("RR_ORDERED_DATE", reportingRestrictionOrderedDate);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

}
