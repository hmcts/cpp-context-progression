package uk.gov.moj.cpp.progression.service.amp.service;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@SuppressWarnings({"squid:S2139", "squid:S00112", "squid:S2142"})
public class AmpClientService {

    @Inject
    @Value(key = "restEasyClientConnectionPoolSize", defaultValue = "10")
    private String restEasyClientConnectionPoolSize;

    ResteasyClient client;

    @PostConstruct
    public void createClient() {
        client = new ResteasyClientBuilderImpl().disableTrustManager()
                .connectionPoolSize(Integer.parseInt(restEasyClientConnectionPoolSize))
                .build();
    }

    public Response post(final String url, final PcrEventPayload payload) {
        final Invocation.Builder request = this.client.target(url).request();
        request.headers(new MultivaluedHashMap(Map.of(CONTENT_TYPE, MediaType.APPLICATION_JSON)));
        return request.post(Entity.json(payload));
    }
}
