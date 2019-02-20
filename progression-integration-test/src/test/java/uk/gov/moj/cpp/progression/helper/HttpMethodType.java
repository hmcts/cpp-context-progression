package uk.gov.moj.cpp.progression.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import uk.gov.justice.services.common.http.HeaderConstants;

import com.github.tomakehurst.wiremock.client.MappingBuilder;

@SuppressWarnings("squid:S1133")
public enum HttpMethodType {
    GET {
        @Override
        @Deprecated
        public MappingBuilder getMappingBuilder(final String requestUri, final int responseStatus, final String body) {
            return get(urlMatching(requestUri))
                    .willReturn(aResponse()
                            .withStatus(responseStatus)
                            .withHeader("CPPID", "3f4832b2-2e50-11e6-b67b-9e71128cae77")
                            .withHeader("Content-Type", "application/json")
                            .withBody(body));
        }

        @Override
        public MappingBuilder getMappingBuilder(final String requestUri, final int responseStatus, final String body, final String userId) {
            return get(urlMatching(requestUri))
                    .withHeader(HeaderConstants.USER_ID, equalTo(userId))
                    .willReturn(aResponse()
                            .withStatus(responseStatus)
                            .withHeader("CPPID", "3f4832b2-2e50-11e6-b67b-9e71128cae77")
                            .withHeader("Content-Type", "application/json")
                            .withBody(body));
        }


    }, POST {
        @Override
        @Deprecated
        public MappingBuilder getMappingBuilder(final String requestUri, final int responseStatus, final String body) {
            return post(urlMatching(requestUri))
                    .willReturn(aResponse().withStatus(responseStatus)
                            .withHeader("Content-Type", "application/json")
                            .withBody(body));
        }

        @Override
        public MappingBuilder getMappingBuilder(final String requestUri, final int responseStatus, final String body, final String userId) {
            return post(urlMatching(requestUri))
                    .withHeader(HeaderConstants.USER_ID, equalTo(userId))
                    .willReturn(aResponse().withStatus(responseStatus)
                            .withHeader("Content-Type", "application/json")
                            .withBody(body));
        }
    };

    @Deprecated
    public abstract MappingBuilder getMappingBuilder(String requestUri, int responseStatus, String body);

    public abstract MappingBuilder getMappingBuilder(String requestUri, int responseStatus, String body, String userId);
}
