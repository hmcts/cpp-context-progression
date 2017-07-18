package uk.gov.justice.api.resource;

import uk.gov.justice.services.adapter.rest.application.DefaultCommonProviders;

import java.util.Set;

import javax.enterprise.inject.Specializes;

/**
 * Register non-framework compliant REST resources.
 * Part of a workaround used until the Framework can support non-JSON responses.
 */
@Specializes
public class ProgressionCommandApiCommonProviders extends DefaultCommonProviders {

    @Override
    public Set<Class<?>> providers() {
        Set<Class<?>> classes = super.providers();
        classes.add(DefaultCasesCaseidCasedocumentsResource.class);
        return classes;
    }
}
