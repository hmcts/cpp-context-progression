package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceIndicatedPlea;
import uk.gov.moj.cpp.progression.persistence.entity.OffencePlea;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class OffenceForDefendantUpdatedToEntity implements Converter<OffenceForDefendant, OffenceDetail> {
    @Override
    public OffenceDetail convert(final OffenceForDefendant event) {
        return new OffenceDetail.OffenceDetailBuilder().setId(event.getId())
                .setCode(event.getOffenceCode())
                .setWording(event.getWording())
                .withOffenceIndicatedPlea(getOffenceIndicatedPlea(event.getOffenceIndicatedPlea()))
                .setOffencePlea(getOffencePlea(event.getOffencePlea()))
                .withSection(event.getSection())
                .setStartDate(event.getStartDate())
                .setEndDate(event.getEndDate())
                .setConvictionDate(event.getConvictionDate())
                .withOrderIndex(event.getOrderIndex())
                .withCount(event.getCount()).build();
    }

    private OffencePlea getOffencePlea(final uk.gov.moj.cpp.progression.domain.event.defendant.OffencePlea offencePlea) {
        OffencePlea offencePleaEntity = null;
        if (offencePlea != null) {
            offencePleaEntity = new OffencePlea(offencePlea.getId(), offencePlea.getValue(),
                    offencePlea.getPleaDate());
        }
        return offencePleaEntity;
    }

    private OffenceIndicatedPlea getOffenceIndicatedPlea(
            final uk.gov.moj.cpp.progression.domain.event.defendant.OffenceIndicatedPlea offenceIndicatedPlea) {
        OffenceIndicatedPlea offenceIndicatedPleaEntity = null;
        if (offenceIndicatedPlea != null) {
            offenceIndicatedPleaEntity = new OffenceIndicatedPlea(offenceIndicatedPlea.getId(),
                    offenceIndicatedPlea.getValue(),
                    offenceIndicatedPlea.getAllocationDecision());
        }
        return offenceIndicatedPleaEntity;
    }
}
