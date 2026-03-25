package uk.gov.moj.cpp.progression.util;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.jmx.api.mbean.CommandRunMode;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.jmx.api.mbean.CommandRunMode.FORCED;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.management.suspension.commands.RefreshFeatureControlCacheCommand.REFRESH_FEATURE_CACHE;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonBuilderFactory;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.Map;
import java.util.UUID;

public class FeatureStubUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStubUtil.class);

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int PORT = 9990;
    private static final TestSystemCommanderClientFactory systemCommanderClientFactory = new TestSystemCommanderClientFactory();

    private static final UUID NULL_COMMAND_RUNTIME_ID = null;
    private static final String NULL_COMMAND_RUNTIME_STRING = null;
    private static final CommandRunMode FORCED_COMMAND_RUN_MODE = FORCED;

    public static void setFeatureToggle(final String featureName, final boolean isToggleOn) {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of(featureName, isToggleOn);
        clearCache(CONTEXT_NAME);
        stubFeaturesFor(CONTEXT_NAME, features);
    }

    /**
     * Provides the capability to stub out the features in Azure Feature Manager and toggle them on and off.
     *
     * @param contextName name of context that the tests are running in
     * @param features    map of features to toggle with their enabled status.
     *                    The key is the feature name; the value is its enabled status.
     *
     */
    public static void stubFeaturesFor(final String contextName, final Map<String, Boolean> features) {
        final JsonObject featuresJson = getJsonBuilderFactory().createObjectBuilder()
                .add("items", buildFeaturesAzurePayload(features))
                .build();

        LOGGER.info("Stubbed payload from Azure feature manager: {}", featuresJson);

        stubFor(get(urlPathMatching("/azure/featuremanager/kv/.*"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(featuresJson.toString())));

        clearCache(contextName);
    }

    /**
     * Provides a hook for the tester to clear out the cache for the features.
     *
     * @param contextName name of context that the tests are running in
     *
     */
    public static void clearCache(final String contextName) {
        LOGGER.info("Clearing feature cache for: {}", contextName);

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .withUsername("admin")
                .withPassword("admin")
                .build();


        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(jmxParameters)) {
            systemCommanderClient.getRemote(contextName).call(REFRESH_FEATURE_CACHE, NULL_COMMAND_RUNTIME_ID, NULL_COMMAND_RUNTIME_STRING, FORCED_COMMAND_RUN_MODE.isGuarded());
        }
    }

    private static JsonArrayBuilder buildFeaturesAzurePayload(final Map<String, Boolean> features) {
        final JsonArrayBuilder featuresArrayBuilder = getJsonBuilderFactory().createArrayBuilder();

        features.forEach((feature, isEnabled) ->
                featuresArrayBuilder.add(getJsonBuilderFactory().createObjectBuilder()
                        .add("etag", "p09XKJF7DC7F4CN3gPSsOGAeU5c")
                        .add("key", format(".appconfig.featureflag/%s", feature))
                        .add("label", "STE11")
                        .add("content_type", "application/vnd.microsoft.appconfig.ff+json;charset=utf-8")
                        .add("value", format("{\"id\":\"%s\",\"description\":\"Sample Feature\",\"enabled\":%s,\"conditions\":{\"client_filters\":[]}}", feature, isEnabled))
                        .add("locked", false)
                        .add("last_modified", "2020-11-02T10:03:33+00:00")
                        .build())
        );

        return featuresArrayBuilder;
    }
}
