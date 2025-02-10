package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupDefenceClientPermission;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.File;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
@ExtendWith(JmsResourceManagementExtension.class)
public abstract class AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestHelper.class);

    public static final String USER_ID = randomUUID().toString();
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://" + HOST + ":8080");
    private static final String WRITE_BASE_URL = "/progression-service/command/api/rest/progression";
    private static final String READ_BASE_URL = "/progression-service/query/api/rest/progression";

    protected final RestClient restClient = new RestClient();

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    protected void makePostCall(final String url, final String mediaType, final String payload) {
        makePostCall(url, mediaType, payload, Response.Status.ACCEPTED.getStatusCode());
    }

    protected void makePostCall(final String url, final String mediaType, final String payload, final int statusCode) {
        makePostCall(UUID.fromString(USER_ID), url, mediaType, payload, statusCode);
    }

    protected void makePostCall(final UUID userId, final String url, final String mediaType, final String payload, final int statusCode) {
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\nUser ID = {}", url, mediaType, payload, USER_ID);
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        try (Response response = restClient.postCommand(url, mediaType, payload, map)) {
            assertThat(response.getStatus(), is(statusCode));
        }
    }

    protected UUID makeMultipartFormPostCall(final UUID userId, final String url, final String fileFieldName, final String fileName) {
        final File file = new File(fileName);
        final UUID correlationId = randomUUID();
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, userId.toString());
        headers.add(HeaderConstants.CLIENT_CORRELATION_ID, correlationId);
        final MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData(fileFieldName, file, MediaType.MULTIPART_FORM_DATA_TYPE, file.getName());
        final GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<>(mdo) {
        };
        try (Response response = ResteasyClientBuilderFactory.clientBuilder().build().target(getWriteUrl(url)).request().headers(headers).post(
                Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE))) {
            assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        }
        return correlationId;
    }

    public void stubForDefence(UUID defendantId) {

        final String organisationId = randomUUID().toString();

        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-chamber-groups-by-user.json");
        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId);


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", USER_ID)
                .replace("%DEFENDANT_ID%", defendantId.toString())
                .replace("%ORGANISATION_ID%", randomUUID().toString());

        stubUserGroupOrganisation(USER_ID, organisation);
        stubUserGroupDefenceClientPermission(permission);

    }

    public UUID makeMultipartFormPostCall(final String url, final String fileFieldName, final String fileName) {
        return makeMultipartFormPostCall(UUID.fromString(USER_ID), url, fileFieldName, fileName);
    }

    public void resetUserRoles() {
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-groups-by-user.json");
    }

}
