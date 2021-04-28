package uk.gov.moj.cpp.progression.converter;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.services.common.converter.Converter;

/**
 *
 * @deprecated Hearing can be multi-day so sittingDay should not be always the first value
 *  Multi-day support will be within amendreshare feature.
 *
 */
@Deprecated
public class SeedingHearingConverter implements Converter<Hearing, SeedingHearing> {

    @Override
    public SeedingHearing convert(final Hearing hearing) {
        return SeedingHearing.seedingHearing()
                .withSeedingHearingId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withSittingDay(hearing.getHearingDays().get(0).getSittingDay().toLocalDate().toString())
                .build();
    }
}
