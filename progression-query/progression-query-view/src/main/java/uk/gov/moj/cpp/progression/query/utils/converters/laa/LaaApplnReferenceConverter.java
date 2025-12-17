package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.progression.query.laa.LaaApplnReference;
@SuppressWarnings("squid:S1168")
public class LaaApplnReferenceConverter extends LAAConverter {

    public LaaApplnReference convert(final LaaReference laaApplnReference) {
        if (isNull(laaApplnReference)) {
            return null;
        }
        return LaaApplnReference.laaApplnReference()
                .withApplicationReference(laaApplnReference.getApplicationReference())
                .withStatusCode(laaApplnReference.getStatusCode())
                .withStatusDescription(laaApplnReference.getStatusDescription())
                .withStatusId(laaApplnReference.getStatusId())
                .build();
    }
}