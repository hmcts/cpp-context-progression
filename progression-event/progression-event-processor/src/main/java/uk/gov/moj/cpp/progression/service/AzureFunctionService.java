package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.io.IOException;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class AzureFunctionService {

    private static final String HOST_NAME = System.getProperty("AZURE_FUNCTION_HOST_NAME", "fa-ste-casefilter.azurewebsites.net");
    private static final String SET_CASE_EJECTED_FUNCTION_URL = "https://" + HOST_NAME + "/api/setCaseEjected?code=6o94LtYzbEBjHHWJWAcrHjFnUfG5ttkUOHqaJQUAfIiCx27D6G8AZQ==";
    private static final String SET_CASE_EJECTED_FUNCTION_CONTENT_TYPE = "application/json";

    public Integer makeFunctionCall(final String payload) throws IOException {

        HttpPost post = new HttpPost(SET_CASE_EJECTED_FUNCTION_URL);
        post.addHeader("content-type", SET_CASE_EJECTED_FUNCTION_CONTENT_TYPE);
        post.addHeader(HeaderConstants.USER_ID, UUID.randomUUID().toString());
        post.setEntity(new StringEntity(payload));

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {

            return response.getStatusLine().getStatusCode();
        }
    }
}
