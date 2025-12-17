package uk.gov.moj.cpp.progression.command.api.interceptors;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.adapter.rest.interceptor.InputStreamFileInterceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;

import java.util.ArrayList;
import java.util.List;

public class ProgressionCommandApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    @Override
    public String component() {
        return COMMAND_API;
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        final List<InterceptorChainEntry> interceptorChainEntries = new ArrayList<>();
        interceptorChainEntries.add(new InterceptorChainEntry(6000, InputStreamFileInterceptor.class));
        return interceptorChainEntries;
    }
}