package uk.gov.moj.cpp.progression.event.converter;

import static java.util.stream.Collectors.toSet;

import java.util.Set;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.progression.domain.event.defendant.CPR;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffenderDomain;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.persistence.entity.Address;
import uk.gov.moj.cpp.progression.persistence.entity.CPRDetails;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantOffenderDetails;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Person;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class DefendantAddedToDefendant implements Converter<DefendantAdded, Defendant> {

    @Override
    public Defendant convert(final DefendantAdded defendantAdded) {

        final Set<OffenceDetail> offences = defendantAdded.getOffences().stream()
                .map(DefendantAddedToDefendant::mapToOffenceDetails)
                .collect(toSet());

        final Person person =  getPersonDetail(defendantAdded.getPerson());

        return new Defendant(defendantAdded.getDefendantId(), person, defendantAdded.getPoliceDefendantId(),
                offences,false);

    }

    private Person getPersonDetail(final uk.gov.moj.cpp.progression.domain.event.defendant.Person person) {

        if(person == null) {
            return null;
        }

        final Address address = getAddressDetail(person);

        return new Person().builder()
                .personId(person.getId())
                .title(person.getTitle())
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .dateOfBirth(person.getDateOfBirth())
                .nationality(person.getNationality())
                .gender(person.getGender())
                .homeTelephone(person.getHomeTelephone())
                .workTelephone(person.getWorkTelephone())
                .fax(person.getFax())
                .mobile(person.getMobile())
                .email(person.getEmail())
                .address(address).build();
    }

    private Address getAddressDetail(final uk.gov.moj.cpp.progression.domain.event.defendant.Person person) {
        final Address address = new Address();
        if(person.getAddress() != null){
            address.setAddress1(person.getAddress().getAddress1());
            address.setAddress2(person.getAddress().getAddress2());
            address.setAddress3(person.getAddress().getAddress3());
            address.setAddress4(person.getAddress().getAddress4());
            address.setPostCode(person.getAddress().getPostCode());
        }
        return address;
    }

    private static OffenceDetail mapToOffenceDetails(final Offence offence) {
        final CPR cpr = offence.getCpr();
        final DefendantOffenderDomain defendantOffenderDomain = cpr.getDefendantOffender();

        final DefendantOffenderDetails defendantOffenderDetails = new DefendantOffenderDetails(defendantOffenderDomain.getYear(),
                defendantOffenderDomain.getOrganisationUnit(), defendantOffenderDomain.getNumber(),
                defendantOffenderDomain.getCheckDigit());

        final CPRDetails cprDetails = new CPRDetails(defendantOffenderDetails);

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
