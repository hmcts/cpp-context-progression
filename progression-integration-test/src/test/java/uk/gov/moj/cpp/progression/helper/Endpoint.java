package uk.gov.moj.cpp.progression.helper;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.http.HttpStatus;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.function.Function;

public class Endpoint {

    private final Function<UrlMatchingStrategy, MappingBuilder> requestType;
    private final String url;
    private final String contentType;
    private final int status;
    private final JsonObject body;

    private Endpoint(Function<UrlMatchingStrategy, MappingBuilder> requestType, String url,
                    String contentType, int status, JsonObject body) {
        this.requestType = requestType;
        this.url = url;
        this.contentType = contentType;
        this.status = status;
        this.body = body;
    }

    public Function<UrlMatchingStrategy, MappingBuilder> getRequestType() {
        return requestType;
    }

    public String getContentType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public JsonObject getBody() {
        return body;
    }

    public static class EndpointBuilder {

        private Function<UrlMatchingStrategy, MappingBuilder> requestType = WireMock::get;
        private String url;
        private String contentType = "application/json";
        private int status = HttpStatus.SC_OK;
        private JsonObject body = Json.createObjectBuilder().build();

        public EndpointBuilder endpoint(String url) {
            this.url = url;
            return this;
        }

        public EndpointBuilder forRequestType(
                        Function<UrlMatchingStrategy, MappingBuilder> requestType) {
            this.requestType = requestType;
            return this;
        }

        public EndpointBuilder willReturnStatus(int status) {
            this.status = status;
            return this;
        }

        public EndpointBuilder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public EndpointBuilder andBody(JsonObject body) {
            this.body = body;
            return this;
        }

        public Endpoint build() {
            return new Endpoint(requestType, url, contentType, status, body);
        }
    }
}
