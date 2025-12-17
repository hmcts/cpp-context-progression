package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.progression.query.laa.LaaApplnReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaaApplnReferenceConverterTest {

    @InjectMocks
    private LaaApplnReferenceConverter laaApplnReferenceConverter;

    @Test
    void shouldReturnNullWhenLaaReferenceIsNull() {
        final LaaApplnReference result = laaApplnReferenceConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertLaaReference() {
        final LaaReference laaApplnReference = LaaReference.laaReference()
                .withApplicationReference("application reference")
                .withStatusCode("status code")
                .withStatusDescription("status description")
                .withStatusId(randomUUID())
                .build();

        final LaaApplnReference result = laaApplnReferenceConverter.convert(laaApplnReference);

        assertThat(result.getApplicationReference(), is(laaApplnReference.getApplicationReference()));
        assertThat(result.getStatusCode(), is(laaApplnReference.getStatusCode()));
        assertThat(result.getStatusDescription(), is(laaApplnReference.getStatusDescription()));
        assertThat(result.getStatusId(), is(laaApplnReference.getStatusId()));
    }

    @Test
    void shouldConvertLaaReferenceWhenThereAreNullValues() {
        final LaaReference laaApplnReference = LaaReference.laaReference()
                .withApplicationReference("application reference")
                .withStatusCode("status code")
                .withStatusDescription("status description")
                .build();

        final LaaApplnReference result = laaApplnReferenceConverter.convert(laaApplnReference);

        assertThat(result.getApplicationReference(), is(laaApplnReference.getApplicationReference()));
        assertThat(result.getStatusCode(), is(laaApplnReference.getStatusCode()));
        assertThat(result.getStatusDescription(), is(laaApplnReference.getStatusDescription()));
        assertThat(result.getStatusId(), nullValue());
    }

}