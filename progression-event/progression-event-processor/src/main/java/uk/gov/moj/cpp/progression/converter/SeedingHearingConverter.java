package uk.gov.moj.cpp.progression.converter;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.services.common.converter.Converter;


public class SeedingHearingConverter implements Converter<Hearing, SeedingHearing> {

    @Override
    public SeedingHearing convert(final Hearing hearing) {
        return SeedingHearing.seedingHearing()
                .withSeedingHearingId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType())
                .build();
    }
}
