package uk.gov.moj.cpp.progression;

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.WireMockStubUtils.mockMaterialUpload;
import static uk.gov.moj.cpp.progression.WireMockStubUtils.setupAsSystemUser;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.io.Resources;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIT.class);
    protected static final UUID USER_ID_VALUE = randomUUID();
    protected static final UUID USER_ID_VALUE_AS_ADMIN = randomUUID();

    public static final Header CPP_UID_HEADER = new Header(USER_ID, USER_ID_VALUE.toString());
    protected static final Header CPP_UID_HEADER_AS_ADMIN = new Header(USER_ID, USER_ID_VALUE_AS_ADMIN.toString());

    private static final String ENDPOINT_PROPERTIES_FILE = "endpoint.properties";
    protected static final Properties ENDPOINT_PROPERTIES = new Properties();
    protected static final String PUBLIC_EVENT_TOPIC = "public.event";
    protected static final String APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON = "application/vnd.progression.query.courtdocuments+json";


    protected static RequestSpecification requestSpec;
    protected static String baseUri;
    protected static RestClient restClient = new RestClient();

    private static final ThreadLocal<UUID> USER_CONTEXT = new ThreadLocal<>();

    @Before
    public void setUp() {
        readConfig();
        setRequestSpecification();
        WireMockStubUtils.setupAsAuthorisedUser(USER_ID_VALUE);
        setupAsSystemUser(USER_ID_VALUE_AS_ADMIN);
        AuthorisationServiceStub.stubEnableAllCapabilities();
        mockMaterialUpload();
    }

    protected JSONObject getExistingHearing(final String hearingId) {
        final String queryAPIEndPoint = MessageFormat
                .format(ENDPOINT_PROPERTIES.getProperty("hearing.get.hearing"), hearingId.toString());

        final String url = getBaseUri() + "/" + queryAPIEndPoint;
        final String mediaType = "application/vnd.hearing.get.hearing+json";

        final String payload = poll(requestParams(url, mediaType).withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue()).build())
                .until(status().is(OK)).getPayload();
        return new JSONObject(payload);
    }


    protected static void setLoggedInUser(final UUID userId) {
        USER_CONTEXT.set(userId);
    }

    protected static UUID getLoggedInUser() {
        return USER_CONTEXT.get();
    }

    protected static MultivaluedMap<String, Object> getLoggedInHeader() {
        final MultivaluedMap<String, Object> header = new MultivaluedHashMap<>();
        header.add(USER_ID, getLoggedInUser().toString());
        return header;
    }


    protected static void readConfig() {

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(ENDPOINT_PROPERTIES_FILE)) {
            ENDPOINT_PROPERTIES.load(stream);
        } catch (final IOException e) {
            LOGGER.warn("Error reading properties from {}", ENDPOINT_PROPERTIES_FILE, e);
        }
        final String baseUriProp  = System.getProperty("INTEGRATION_HOST_KEY");
        baseUri = isNotEmpty(baseUriProp) ? format("http://%s:8080", baseUriProp) : ENDPOINT_PROPERTIES.getProperty("base-uri");
    }

    private static void setRequestSpecification() {
        requestSpec = new RequestSpecBuilder().setBaseUri(baseUri).build();
    }

    protected static Matcher<String> equalStr(final Object bean, final String name) {
        return equalTo(getString(bean, name));
    }

    protected static Matcher<String> equalStr(final Object bean, final char separator, final String... names) {
        return equalTo(StringUtils.join(Stream.of(names).map(name -> getString(bean, name).trim()).collect(toList()), separator).trim());
    }

    protected static Matcher<String> equalStr(final Object bean, final String name, final DateTimeFormatter dateTimeFormatter) {
        return equalTo(getString(bean, name, dateTimeFormatter));
    }

    protected static Matcher<Integer> equalInt(final Object bean, final String name) {
        return equalTo(getInteger(bean, name));
    }

    protected static Matcher<String> equalDate(final Temporal localDate) {
        return equalTo(ISO_LOCAL_DATE.format(localDate));
    }

    protected static Matcher<String> equalEnum(final Enum<?> e) {
        return equalTo(e.name());
    }

    protected static String getString(final Object bean, final String name, final DateTimeFormatter dateTimeFormatter) {
        try {
            final Temporal dateTime = (Temporal) PropertyUtils.getNestedProperty(bean, name);
            if (dateTime instanceof LocalDate) {
                return dateTimeFormatter.format(((LocalDate) dateTime).atStartOfDay());
            }
            return dateTimeFormatter.format(dateTime);
        } catch (final Exception e) {
            LOGGER.error("Cannot get string property: " + name + " from bean " + bean.getClass().getCanonicalName(), e.getMessage(), e);
            return EMPTY;
        }
    }

    protected static UUID getUUID(final Object bean, final String name) {
        return UUID.fromString(getString(bean, name));
    }

    protected static String getString(final Object bean, final String name) {
        try {
            return EMPTY + PropertyUtils.getNestedProperty(bean, name);
        } catch (final Exception e) {
            LOGGER.error("Cannot get string property: " + name + " from bean " + bean.getClass().getCanonicalName(), e.getMessage(), e);
            return EMPTY;
        }
    }

    protected static Integer getInteger(final Object bean, final String name) {
        try {
            return Integer.parseInt(getString(bean, name));
        } catch (final Exception e) {
            LOGGER.error("Cannot get integer property: " + name + " from bean " + bean.getClass().getCanonicalName(), e.getMessage(), e);
            return null;
        }
    }

    protected static String getStringFromResource(final String path) throws IOException {
        return Resources.toString(getResource(path), defaultCharset());
    }

    public static String getURL(final String property, final Object... args) {
        return getBaseUri() + "/" + MessageFormat.format(ENDPOINT_PROPERTIES.getProperty(property), args);
    }

    public static Matcher<ResponseData> print() {
        return new BaseMatcher<ResponseData>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof ResponseData) {
                    final ResponseData responseData = (ResponseData) o;
                    System.out.println(responseData.getPayload());
                }
                return true;
            }

            @Override
            public void describeTo(final Description description) {
            }
        };
    }


    protected static RequestParams requestParameters(final String url, final String contentType) {
        return requestParams(url, contentType).withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue()).build();
    }
}
