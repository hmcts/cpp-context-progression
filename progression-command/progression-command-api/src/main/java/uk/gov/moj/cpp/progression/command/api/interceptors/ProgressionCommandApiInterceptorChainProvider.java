package uk.gov.moj.cpp.progression.command.api.interceptors;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.adapter.rest.interceptor.InputStreamFileInterceptor;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChainProvider;
import uk.gov.moj.cpp.authorisation.interceptor.SynchronousFeatureControlInterceptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class ProgressionCommandApiInterceptorChainProvider implements InterceptorChainProvider {

    @Override
    public String component() {
        return COMMAND_API;
    }

    @Override
    public List<Pair<Integer, Class<? extends Interceptor>>> interceptorChainTypes() {
        final ArrayList<Pair<Integer, Class<? extends Interceptor>>> pairs = new ArrayList<>();
        pairs.add(new ImmutablePair<>(5900, SynchronousFeatureControlInterceptor.class));
        pairs.add(new ImmutablePair<>(6000, InputStreamFileInterceptor.class));
        return pairs;
    }
}