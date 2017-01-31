package uk.gov.justice.api.resource;

import uk.gov.justice.services.adapter.rest.application.CommonProviders;

import javax.enterprise.inject.Specializes;
import java.util.Set;

/**
 * Register non-framework compliant REST resources.
 * Part of a workaround used until the Framework can support non-JSON responses.
 */
@Specializes
public class ProgressionCommandApiCommonProviders extends CommonProviders {

    @Override
    public Set<Class<?>> providers() {
        Set<Class<?>> classes = super.providers();
        classes.add(DefaultCasesCaseidCasedocumentsResource.class);
        return classes;
    }
}
