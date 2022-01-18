package uk.gov.moj.cpp.progression.service;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpClientWrapper {
    public CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }
}

