package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.File;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import com.jayway.restassured.path.json.JsonPath;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractTestHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestHelper.class);

    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
    public static final String USER_ID = UUID.randomUUID().toString();
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY","localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://"+HOST+":8080");
    protected static final String STRUCTURE_EVENT_TOPIC = "progression.event";
    private static final String WRITE_BASE_URL = "/progression-service/command/api/rest/progression";
    private static final String READ_BASE_URL = "/progression-service/query/api/rest/progression";

    protected final RestClient restClient = new RestClient();

    protected MessageConsumerClient publicConsumer = new MessageConsumerClient();
    protected MessageConsumer privateEventsConsumer;
    protected MessageConsumer publicEventsConsumer;

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    static {
        doAllStubbing();
    }

    public static void doAllStubbing() {
        RestHelper.createMockEndpoints();
    }

    protected void makePostCall(final String url, final String mediaType, final String payload) {
        makePostCall(url, mediaType, payload , Response.Status.ACCEPTED.getStatusCode());
    }

    protected void makePostCall(final String url, final String mediaType, final String payload, final int statusCode) {
        makePostCall(UUID.fromString(USER_ID), url, mediaType, payload , statusCode);
    }

    protected void makePostCall(final UUID userId, final String url, final String mediaType, final String payload, final int statusCode ) {
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", url, mediaType, payload, USER_ID);
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        final Response response = restClient.postCommand(url, mediaType, payload, map);
        assertThat(response.getStatus(), is(statusCode));
    }
    
    protected UUID makeMultipartFormPostCall(final UUID userId, final String url, final String fileFieldName, final String fileName) {
        final File file = new File(fileName);
        final UUID correlationId = randomUUID();
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, userId.toString());
        headers.add(HeaderConstants.CLIENT_CORRELATION_ID, correlationId);
        final MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData(fileFieldName, file, MediaType.MULTIPART_FORM_DATA_TYPE, file.getName());
        final GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
        };
        final Response response = ResteasyClientBuilderFactory.clientBuilder().build().target(getCommandUri(url)).request().headers(headers).post(
                Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE)
        );
        response.close();
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
        return correlationId;
    }

    public UUID makeMultipartFormPostCall(final String url, final String fileFieldName, final String fileName) {
    	return makeMultipartFormPostCall(UUID.fromString(USER_ID),url, fileFieldName, fileName);
    }

    protected void makePostCallWithUserId(final String url, final String mediaType, final String payload, final String userId) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, userId);
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tHeaders = {} \n\tPayload = {}\n\n", url, mediaType, headers, payload, userId);
        final Response response = restClient.postCommand(url, mediaType, payload, headers);
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    protected Response makeGetCall(final String url, final String mediaType) {
        return makeGetCall(url, mediaType, USER_ID);
    }

    protected Response makeGetCall(final String url, final String mediaType, final String userId) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        //FIXME: bug in framework requiring the media type to be added explicitly when using this API call on RestClient
        map.add(HttpHeaders.ACCEPT, mediaType);
        final Response response = restClient.query(url, mediaType, map);
        LOGGER.info("Get call made: \n\n\tEndpoint = {} \n\tMedia type = {}\n\n", url, mediaType, userId);
        return response;
    }

    /**
     * Additional verification method for each helper class to ensure that query operations are
     * restricted.  Each concrete sub-class should override this appropriately
     */
    public void assertQueryCallResponseStatusIs(final Response.Status status) {

    }

    public JsonPath getMessage() {
        return retrieveMessage(privateEventsConsumer);
    }

    public JsonPath getMessage(final long customTimeoutInMillis) {
        return retrieveMessage(privateEventsConsumer, customTimeoutInMillis).orElse(null);
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
