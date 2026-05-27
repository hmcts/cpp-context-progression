package uk.gov.moj.cpp.progression.healthchecks;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.healthcheck.healthchecks.FileStoreHealthcheck.FILE_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionIgnoredHealthcheckNamesProviderTest {


    @InjectMocks
    private ProgressionIgnoredHealthcheckNamesProvider ignoredHealthcheckNamesProvider;

    @Test
    public void shouldIgnoreFileStoreAndJobStoreHealthchecks() throws Exception {

        final List<String> namesOfIgnoredHealthChecks = ignoredHealthcheckNamesProvider.getNamesOfIgnoredHealthChecks();

        assertThat(namesOfIgnoredHealthChecks.size(), is(2));
        assertThat(namesOfIgnoredHealthChecks, hasItems(JOB_STORE_HEALTHCHECK_NAME, FILE_STORE_HEALTHCHECK_NAME));
    }
}