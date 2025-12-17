package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Objects.isNull;

import uk.gov.justice.progression.query.laa.HearingType;
@SuppressWarnings("squid:S1168")
public class HearingTypeConverter extends LAAConverter {

    public HearingType convert(final uk.gov.justice.core.courts.HearingType hearingType) {
        if (isNull(hearingType)) {
            return null;
        }
        return HearingType.hearingType()
                .withId(hearingType.getId())
                .withDescription(hearingType.getDescription())
                .build();
    }
}