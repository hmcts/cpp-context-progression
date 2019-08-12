package uk.gov.moj.cpp.progression.it.framework.util;

import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.jmx.api.command.CatchupCommand;
import uk.gov.justice.services.jmx.api.command.RebuildCommand;
import uk.gov.justice.services.jmx.api.command.ShutterSystemCommand;
import uk.gov.justice.services.jmx.api.command.UnshutterSystemCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

public class SystemCommandInvoker {

    private static final String HOST = getHost();
    private static final int JMX_PORT = 9990;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String SERVICE_NAME = CONTEXT_NAME + "-service";

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    public void invokeRebuild() {

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameter())) {
            systemCommanderClient.getRemote(SERVICE_NAME).call(new RebuildCommand());
        }
    }

    public void invokeCatchup() {

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameter())) {
            systemCommanderClient.getRemote(SERVICE_NAME).call(new CatchupCommand());
        }
    }

    public void invokeShutter() {

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameter())) {
            systemCommanderClient.getRemote(SERVICE_NAME).call(new ShutterSystemCommand());
        }
    }

    public void invokeUnshutter() {

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameter())) {
            systemCommanderClient.getRemote(SERVICE_NAME).call(new UnshutterSystemCommand());
        }
    }

    private JmxParameters jmxParameter() {
        return jmxParameters()
                .withContextName(SERVICE_NAME)
                .withHost(HOST)
                .withPort(JMX_PORT)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .build();
    }
}
