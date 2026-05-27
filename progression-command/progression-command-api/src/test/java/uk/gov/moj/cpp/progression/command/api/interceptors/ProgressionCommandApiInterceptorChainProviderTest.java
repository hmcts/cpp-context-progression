package uk.gov.moj.cpp.progression.command.api.interceptors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ProgressionCommandApiInterceptorChainProviderTest {

    private final ProgressionCommandApiInterceptorChainProvider provider = new ProgressionCommandApiInterceptorChainProvider();

    @Test
    public void shouldReturnCommandApiAsComponent() {
        assertThat(provider.component(), is(COMMAND_API));
    }

    @Test
    public void shouldContainProgressionServiceFileInterceptorAtPriority6000() {
        final List<InterceptorChainEntry> entries = provider.interceptorChainTypes();

        assertThat(entries.size(), is(1));
        assertThat(entries.get(0).getPriority(), is(6000));
        assertThat(entries.get(0).getInterceptorType(), is(ProgressionServiceFileInterceptor.class));
    }
}
