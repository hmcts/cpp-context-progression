package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

public class OffenceForDefendantUpdatedToEntity implements Converter<OffenceForDefendant, OffenceDetail> {
    @Override
    public OffenceDetail convert(OffenceForDefendant event) {
        return new OffenceDetail.OffenceDetailBuilder().setId(event.getId())
                .setCode(event.getOffenceCode())
                .setWording(event.getWording())
                .withIndicatedPlea(event.getIndicatedPlea())
                .withSection(event.getSection())
                .setStartDate(event.getStartDate())
                .setEndDate(event.getEndDate())
                .withOrderIndex(event.getOrderIndex())
                .withCount(event.getCount()).build();
    }
}
