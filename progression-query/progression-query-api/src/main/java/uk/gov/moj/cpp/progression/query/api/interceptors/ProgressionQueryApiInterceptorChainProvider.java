package uk.gov.moj.cpp.progression.query.api.interceptors;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;
import uk.gov.moj.cpp.authorisation.interceptor.SynchronousFeatureControlInterceptor;

import java.util.List;

public class ProgressionQueryApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    @Override
    public String component() {
        return QUERY_API;
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        return newArrayList(new InterceptorChainEntry(5900, SynchronousFeatureControlInterceptor.class));
    }
}