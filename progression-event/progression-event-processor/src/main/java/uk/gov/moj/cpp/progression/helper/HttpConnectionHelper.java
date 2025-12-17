package uk.gov.moj.cpp.progression.helper;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import uk.gov.justice.services.common.http.HeaderConstants;

import java.io.IOException;
import java.util.UUID;

public class HttpConnectionHelper {

    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";

    public Integer getResponseCode(final String url, final String payload) throws IOException {
        final HttpPost post = new HttpPost(url);
        post.addHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE);
        post.addHeader(HeaderConstants.USER_ID, UUID.randomUUID().toString());
        post.setEntity(new StringEntity(payload));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {
            return response.getStatusLine().getStatusCode();
        }
    }
}
