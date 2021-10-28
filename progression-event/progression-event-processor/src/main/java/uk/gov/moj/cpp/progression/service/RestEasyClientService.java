package uk.gov.moj.cpp.progression.service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import uk.gov.justice.services.common.configuration.Value;

public class RestEasyClientService {
    @Inject
    @Value(key = "restEasyClientConnectionPoolSize", defaultValue = "10")
    private String restEasyClientConnectionPoolSize;

    ResteasyClient client;

    @PostConstruct
    public void createClient(){
        client = new ResteasyClientBuilder().disableTrustManager()
                .connectionPoolSize(Integer.parseInt(restEasyClientConnectionPoolSize))
                .build();
    }
    public Response post(final String url,  final String payload, final String key){
        final Invocation.Builder request = this.client.target(url).request();
        request.headers(new MultivaluedHashMap(getHeaders(key)));
        return request.post(Entity.json(payload));
    }

    private Map<String, String> getHeaders(final String subscriptionKey) {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", subscriptionKey,
                "Ocp-Apim-Trace", "true");
    }
}
