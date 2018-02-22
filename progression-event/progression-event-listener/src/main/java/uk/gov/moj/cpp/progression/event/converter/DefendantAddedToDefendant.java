package uk.gov.moj.cpp.progression.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.defendant.CPR;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffenderDomain;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.persistence.entity.CPRDetails;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantOffenderDetails;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class DefendantAddedToDefendant implements Converter<DefendantAdded, Defendant> {

    @Override
    public Defendant convert(DefendantAdded defendantAdded) {

        Set<OffenceDetail> offences = defendantAdded.getOffences().stream()
                .map(DefendantAddedToDefendant::mapToOffenceDetails)
                .collect(toSet());

        return new Defendant(defendantAdded.getDefendantId(), defendantAdded.getPersonId(), defendantAdded.getPoliceDefendantId(),
                offences,false);

    }

    private static OffenceDetail mapToOffenceDetails(Offence offence) {
        CPR cpr = offence.getCpr();
        DefendantOffenderDomain defendantOffenderDomain = cpr.getDefendantOffender();

        DefendantOffenderDetails defendantOffenderDetails = new DefendantOffenderDetails(defendantOffenderDomain.getYear(),
                defendantOffenderDomain.getOrganisationUnit(), defendantOffenderDomain.getNumber(),
                defendantOffenderDomain.getCheckDigit());

        CPRDetails cprDetails = new CPRDetails(defendantOffenderDetails);

        return new OffenceDetail.OffenceDetailBuilder().setId(offence.getId())
                .setPoliceOffenceId(offence.getPoliceOffenceId())
                .setCpr(cprDetails)
                .setWording(offence.getWording())
                .setSequenceNumber(Integer.parseInt(offence.getAsnSequenceNumber()))
                .setCode(offence.getCjsCode())
                .setReason(offence.getReason())
                .setDescription(offence.getDescription())
                .setCode(offence.getCjsCode())
                .setCategory(offence.getCategory())
                .setArrestDate(offence.getArrestDate())
                .setStartDate(offence.getStartDate())
                .setEndDate(offence.getEndDate())
                .setChargeDate(offence.getChargeDate())
                .build();
    }
}
