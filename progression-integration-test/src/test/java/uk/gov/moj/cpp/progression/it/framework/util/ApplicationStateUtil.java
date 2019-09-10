package uk.gov.moj.cpp.progression.it.framework.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.jmx.api.mbean.SystemCommanderMBean;
import uk.gov.justice.services.jmx.api.state.ApplicationManagementState;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.util.Optional;

public class ApplicationStateUtil {

    public static Optional<ApplicationManagementState> getApplicationState(ApplicationManagementState state) {

        final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();
        final Poller poller = new Poller(10, 2000l);

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(getHost())
                .withPort(9990)
                .withUsername("admin")
                .withPassword("admin")
                .build();

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {
            final SystemCommanderMBean systemCommanderMBean = systemCommanderClient.getRemote(CONTEXT_NAME);

            final Optional<ApplicationManagementState> applicationManagementState = poller.pollUntilFound(() -> {
                final ApplicationManagementState applicationState = systemCommanderMBean.getApplicationState();

                if (applicationState == state) {
                    return of(applicationState);
                }

                return empty();
            });

            return applicationManagementState;

        }
    }
}
