package uk.gov.moj.cpp.progression.healthchecks;

import static java.util.List.of;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import uk.gov.justice.services.healthcheck.api.DefaultIgnoredHealthcheckNamesProvider;

import java.util.List;

import javax.enterprise.inject.Specializes;

@Specializes
public class ProgressionIgnoredHealthcheckNamesProvider extends DefaultIgnoredHealthcheckNamesProvider {

    public ProgressionIgnoredHealthcheckNamesProvider() {
        // This constructor is required by CDI.
    }

    @Override
    public List<String> getNamesOfIgnoredHealthChecks() {
        return of(JOB_STORE_HEALTHCHECK_NAME);
    }
}