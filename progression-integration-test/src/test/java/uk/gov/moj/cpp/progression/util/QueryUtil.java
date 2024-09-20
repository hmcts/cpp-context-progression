package uk.gov.moj.cpp.progression.util;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.test.matchers.MapJsonObjectToTypeMatcher;

import java.io.StringReader;
import java.time.LocalDateTime;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

@SuppressWarnings({"squid:S2925"})
public class QueryUtil {


    public static <T> void waitForQueryMatch(final RequestParams requestParams, final long timeout, final BeanMatcher<T> resultMatcher, final Class<T> responseType) {

        final Matcher<ResponseData> expectedConditions = Matchers.allOf(status().is(OK), jsonPayloadMatchesBean(responseType, resultMatcher));

        final LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(timeout);

        ResponseData responseData = makeRequest(requestParams);

        while (!expectedConditions.matches(responseData) && LocalDateTime.now().isBefore(expiryTime)) {
            sleep();
            responseData = makeRequest(requestParams);
        }

        if (!expectedConditions.matches(responseData)) {
            assertThat(responseData, expectedConditions);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
            //ignore
        }
    }

    private static ResponseData makeRequest(final RequestParams requestParams) {
        final Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        final String responseData = response.readEntity(String.class);
        return new ResponseData(Response.Status.fromStatusCode(response.getStatus()), responseData, response.getHeaders());
    }

    public static <T> Matcher<ResponseData> jsonPayloadMatchesBean(final Class<T> theClass, final BeanMatcher<T> beanMatcher) {
        final BaseMatcher<JsonObject> jsonObjectMatcher = MapJsonObjectToTypeMatcher.convertTo(theClass, beanMatcher);
        return new BaseMatcher<ResponseData>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof ResponseData) {
                    final ResponseData responseData = (ResponseData) o;
                    if (responseData.getPayload() != null) {
                        final JsonObject jsonObject = Json.createReader(new StringReader(responseData.getPayload())).readObject();
                        return jsonObjectMatcher.matches(jsonObject);
                    }
                }
                return false;
            }

            @Override
            public void describeMismatch(final Object item, final Description description) {
                final ResponseData responseData = (ResponseData) item;
                final JsonObject jsonObject = Json.createReader(new StringReader(responseData.getPayload())).readObject();
                jsonObjectMatcher.describeMismatch(jsonObject, description);
            }

            @Override
            public void describeTo(final Description description) {
                jsonObjectMatcher.describeTo(description);
            }
        };
    }


}
