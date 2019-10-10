package uk.gov.moj.cpp.progression.helper;

import com.google.common.io.Resources;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.NotifyStub.stubNotifications;
import static uk.gov.moj.cpp.progression.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataOffenceStub.stubReferenceDataOffencesGetOffenceById;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataOffenceStub.stubReferenceDataOffencesGetOffenceByOffenceCode;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubEnforcementArea;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCourtOURoom;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCourtsCodeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryEthinicityData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryHearingTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryJudiciaries;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryNationalityData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryOrganisationUnitsData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryReferralReasons;

public class RestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestHelper.class.getCanonicalName());
    public static RequestSpecification reqSpec;
    private static final Properties prop;
    private static final String baseUri;
    private static String HOST = "localhost";
    private static final int PORT = 8080;
    private static final RestClient restClient = new RestClient();

    static {
        prop = new Properties();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final InputStream stream = loader.getResourceAsStream("endpoint.properties");
        try {
            prop.load(stream);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        final String configuredHost = System.getProperty("INTEGRATION_HOST_KEY");
        if (StringUtils.isNotBlank(configuredHost)) {
            HOST = configuredHost;
        }
        baseUri = (StringUtils.isNotEmpty(HOST) ? "http://" + HOST + ":" + PORT : prop.getProperty("base-uri"));

        reqSpec = new RequestSpecBuilder().setBaseUri(baseUri).build();
    }



    public static javax.ws.rs.core.Response getMaterialContentResponse(final String path, final UUID userId , final String mediaType) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        map.add(HttpHeaders.ACCEPT, mediaType);
        return restClient.query(getQueryUri(path), mediaType, map);
    }

    public static String pollForResponse(final String url,final String path, final String mediaType) {
        return poll(requestParams(baseUri + url + path , mediaType)
                .withHeader("CJSCPPUID", randomUUID().toString()).build())
                .timeout(10, TimeUnit.SECONDS).until(status().is(OK))
                .getPayload();
    }

    public static String pollForResponse(final String path, final String mediaType) {
        return pollForResponse(path, mediaType, status().is(OK));
    }

    public static String pollForResponse(final String path, final String mediaType, final Matcher<ResponseData> matchSuccess) {
        return poll(requestParams(getQueryUri(path), mediaType)
                .withHeader("CJSCPPUID", randomUUID().toString()).build())
                .timeout(10, TimeUnit.SECONDS).until(matchSuccess)
                .getPayload();
    }

    public static String getQueryUri(final String path) {
        return baseUri + prop.getProperty("base-uri-query") + path;
    }

    public static JsonObject getJsonObject(final String jsonAsString) {
        final JsonObject payload;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    public static Response postCommand(final String uri, final String mediaType,
                                       final String jsonStringBody) throws IOException {
        return given().spec(reqSpec).and().contentType(mediaType).body(jsonStringBody)
                .header("CJSCPPUID", randomUUID().toString()).when().post(uri).then()
                .extract().response();
    }

    public static Response getCommand(final String uri, final String mediaType) throws IOException {
        return given().spec(reqSpec).and().accept(mediaType).header("CJSCPPUID", randomUUID().toString()).when().get(uri).then()
                .extract().response();
    }

    public static String getCommandUri(final String path) {
        return baseUri + prop.getProperty("base-uri-command") + path;
    }


    public static void createMockEndpoints() {
        defaultStubs();
        stubQueryEthinicityData("/restResource/ref-data-ethnicities.json",randomUUID());
    }

    private static void defaultStubs() {
        resetStubs();
        setupUsersGroupQueryStub();
        stubEnableAllCapabilities();
        stubQueryCourtsCodeData("/restResource/referencedata.query.local-justice-area-court-prosecutor-mapping-courts.json");
        stubQueryOrganisationUnitsData("/restResource/referencedata.query.organisationunits.json");
        stubListCourtHearing();
        stubReferenceDataOffencesGetOffenceById("/restResource/referencedataoffences.get-offences-by-id.json");
        stubReferenceDataOffencesGetOffenceByOffenceCode("/restResource/referencedataoffences.get-offences-by-offence-code.json");
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubQueryNationalityData("/restResource/ref-data-nationalities.json",randomUUID());
        stubQueryHearingTypeData("/restResource/ref-data-hearing-types.json",randomUUID());
        stubQueryReferralReasons("/restResource/referencedata.query.referral-reasons.json", randomUUID());
        stubQueryJudiciaries("/restResource/referencedata.query.judiciaries.json", randomUUID());
        stubEnforcementArea("/restResource/referencedata.query.enforcement-area.json");
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
        stubQueryCourtOURoom("/restResource/referencedata.ou-courtroom.json");
        stubQueryOrganisation("/restResource/ref-data-get-organisation.json");
        stubNotifications();
        stubMaterialUploadFile();
    }


    public static void createMockEndpointsWithEmpty() {
        defaultStubs();
        stubQueryEthinicityData("/restResource/ref-data-ethnicities-with-noresults.json",randomUUID());
    }

    public static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location " + ramlPath, e);
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    public static void assertThatResponseIndicatesFeatureDisabled(final Response response) {
        assertResponseStatusCode(HttpStatus.SC_FORBIDDEN, response);
    }

    public static void assertThatRequestIsAccepted(final Response response) {
        assertResponseStatusCode(HttpStatus.SC_ACCEPTED, response);
    }

    public static void assertThatResponseIndicatesSuccess(final Response response) {
        assertResponseStatusCode(HttpStatus.SC_OK, response);
    }

    public static void assertResponseStatusCode(final int statusCode, final Response response) {
        assertThat(response.getStatusCode(), equalTo(statusCode));
    }
}
