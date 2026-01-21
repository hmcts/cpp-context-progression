package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.progression.query.laa.SubjectSummary;

import java.util.List;

import javax.inject.Inject;
@SuppressWarnings("squid:S1168")
public class SubjectSummaryLaaConverter extends LAAConverter {

    @Inject
    private DefendantASNConverter defendantASNConverter;

    @Inject
    private DateOfNextHearingConverter dateOfNextHearingConverter;

    @Inject
    private OffenceSummaryConverter offenceSummaryConverter;

    @Inject
    private RepresentationOrderConverter representationOrderConverter;

    public SubjectSummary convert(final CourtApplication courtApplication, final List<Hearing> hearingList) {
        final CourtApplicationParty subject = courtApplication.getSubject();
        return SubjectSummary.subjectSummary()
                .withDefendantASN(courtApplication.getDefendantASN())
                .withDateOfNextHearing(dateOfNextHearingConverter.convert(hearingList))
                .withDefendantDOB(ofNullable(subject.getMasterDefendant()).map(MasterDefendant::getPersonDefendant).map(PersonDefendant::getPersonDetails).map(Person::getDateOfBirth).map(Object::toString).orElse(null))
                .withDefendantFirstName(getDefendantFirstName(subject))
                .withDefendantMiddleName(ofNullable(subject.getMasterDefendant()).map(MasterDefendant::getPersonDefendant).map(PersonDefendant::getPersonDetails).map(Person::getMiddleName).orElse(null))
                .withDefendantLastName(ofNullable(subject.getMasterDefendant()).map(MasterDefendant::getPersonDefendant).map(PersonDefendant::getPersonDetails).map(Person::getLastName).orElse(null))
                .withDefendantNINO(ofNullable(subject.getMasterDefendant()).map(MasterDefendant::getPersonDefendant).map(PersonDefendant::getPersonDetails).map(Person::getNationalInsuranceNumber).orElse(null))
                .withMasterDefendantId(ofNullable(subject.getMasterDefendant()).map(MasterDefendant::getMasterDefendantId).orElse(null))
                .withOffenceSummary(offenceSummaryConverter.convert(courtApplication.getCourtApplicationCases()))
                .withProceedingsConcluded(courtApplication.getProceedingsConcluded())
                .withSubjectId(subject.getId())
                .withRepresentationOrder(representationOrderConverter.convert(subject.getAssociatedDefenceOrganisation()))
                .build();

    }

    private static String getDefendantFirstName(final CourtApplicationParty subject) {
        if (nonNull(subject.getMasterDefendant())) {
            if (nonNull(subject.getMasterDefendant().getPersonDefendant())) {
                if (nonNull(subject.getMasterDefendant().getPersonDefendant().getPersonDetails())) {
                    return subject.getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName();
                }
            } else {
                return subject.getMasterDefendant().getLegalEntityDefendant().getOrganisation().getName();
            }
        }

        return null;
    }

}
