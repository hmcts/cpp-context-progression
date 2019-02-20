package uk.gov.moj.cpp.progression.util;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.test.matchers.MapJsonObjectToTypeMatcher;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.UUID;

@SuppressWarnings({"squid:S2925"})
public class QueryUtil {


    public static  <T> void  waitForQueryMatch(RequestParams requestParams, final long timeout, final BeanMatcher<T> resultMatcher, Class<T> responseType) {

        final Matcher<ResponseData> expectedConditions = Matchers.allOf(status().is(OK), jsonPayloadMatchesBean(responseType, resultMatcher));

        final LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(timeout);

        ResponseData responseData = makeRequest(requestParams);

        System.out.println("responseData: " + responseData.getPayload());

        while (!expectedConditions.matches(responseData) && LocalDateTime.now().isBefore(expiryTime)) {
            sleep();
            responseData = makeRequest(requestParams);
            System.out.println("responseData: " + responseData.getPayload());

        }

        if (!expectedConditions.matches(responseData)) {
            assertThat(responseData, expectedConditions);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private static ResponseData makeRequest(RequestParams requestParams) {
        Response response = new RestClient().query(requestParams.getUrl(), requestParams.getMediaType(), requestParams.getHeaders());
        String responseData = (String) response.readEntity(String.class);
        System.out.println("RESPONSE ::" + responseData);
        return new ResponseData(Response.Status.fromStatusCode(response.getStatus()), responseData, response.getHeaders());
    }

    private static <T> Matcher<ResponseData> jsonPayloadMatchesBean(Class<T> theClass, BeanMatcher<T> beanMatcher) {
        final BaseMatcher<JsonObject> jsonObjectMatcher = MapJsonObjectToTypeMatcher.convertTo(theClass, beanMatcher);
        return new BaseMatcher<ResponseData>() {
            @Override
            public boolean matches(final Object o) {
                if (o instanceof ResponseData) {
                    final ResponseData responseData = (ResponseData) o;
                    if (responseData.getPayload() != null) {
                        JsonObject jsonObject = Json.createReader(new StringReader(responseData.getPayload())).readObject();
                        return jsonObjectMatcher.matches(jsonObject);
                    }
                }
                return false;
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                ResponseData responseData = (ResponseData) item;
                JsonObject jsonObject = Json.createReader(new StringReader(responseData.getPayload())).readObject();
                jsonObjectMatcher.describeMismatch(jsonObject, description);
            }

            @Override
            public void describeTo(final Description description) {
                jsonObjectMatcher.describeTo(description);
            }
        };
    }


}
