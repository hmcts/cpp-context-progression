package uk.gov.justice.services;

import static java.util.Objects.isNull;

import uk.gov.justice.core.courts.ApplicationExternalCreatorType;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.unifiedsearch.client.domain.Address;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;
import uk.gov.justice.services.unifiedsearch.client.domain.SubjectSummary;

import java.time.LocalDate;
import java.util.Optional;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2629"})
public class ApplicationMapper {

    public Application transform(final CourtApplication courtApplication) {
        final Application application = new Application();
        application.setApplicationId(courtApplication.getId().toString());
        application.setApplicationReference(courtApplication.getApplicationReference());

        final ApplicationExternalCreatorType applicationExternalCreatorType = courtApplication.getApplicationExternalCreatorType();
        if (applicationExternalCreatorType != null) {
            application.setApplicationExternalCreatorType(applicationExternalCreatorType.toString());
        }
        final ApplicationStatus applicationStatus = courtApplication.getApplicationStatus();

        if (applicationStatus != null) {
            application.setApplicationStatus(applicationStatus.toString());
        }
        final LocalDate applicationReceivedDate = courtApplication.getApplicationReceivedDate();
        if (applicationReceivedDate != null) {
            application.setReceivedDate(applicationReceivedDate.toString());
        }
        final LocalDate applicationDecisionSoughtByDate = courtApplication.getApplicationDecisionSoughtByDate();
        if (applicationDecisionSoughtByDate != null) {
            application.setDecisionDate(applicationDecisionSoughtByDate.toString());
        }
        final CourtApplicationType type = courtApplication.getType();
        if (type != null) {
            application.setApplicationType(type.getType());
            application.setApplicationTypeCode(type.getCode());
        }
        application.setSubjectSummary(getSubjectSummary(courtApplication.getSubject()));
        return application;
    }

    private SubjectSummary getSubjectSummary(final CourtApplicationParty subject) {
        if (isNull(subject)) {
            return null;
        }
        final SubjectSummary subjectSummary = new SubjectSummary();
        subjectSummary.setSubjectId(subject.getId().toString());
        setPersonDefendantDetails(subjectSummary, subject);
        setCorporateDefendantDetails(subjectSummary, subject);
        return subjectSummary;
    }

    private void setCorporateDefendantDetails(final SubjectSummary subjectSummary, final CourtApplicationParty subject) {
        if (isNull(subject.getMasterDefendant()) ||
                isNull(subject.getMasterDefendant().getMasterDefendantId()) ||
                isNull(subject.getMasterDefendant().getLegalEntityDefendant()) ||
                isNull(subject.getMasterDefendant().getLegalEntityDefendant().getOrganisation())
        ) {
            return;
        }
        subjectSummary.setMasterDefendantId(subject.getMasterDefendant().getMasterDefendantId().toString());
        subjectSummary.setOrganisationName(subject.getMasterDefendant().getLegalEntityDefendant().getOrganisation().getName());
        subjectSummary.setAddress(setAddress(subject.getMasterDefendant().getLegalEntityDefendant().getOrganisation().getAddress()));
    }

    private void setPersonDefendantDetails(final SubjectSummary subjectSummary, final CourtApplicationParty subject) {
        if (isNull(subject.getMasterDefendant()) ||
                isNull(subject.getMasterDefendant().getMasterDefendantId()) ||
                isNull(subject.getMasterDefendant().getPersonDefendant()) ||
                isNull(subject.getMasterDefendant().getPersonDefendant().getPersonDetails())
        ) {
            return;
        }
        subjectSummary.setMasterDefendantId(subject.getMasterDefendant().getMasterDefendantId().toString());
        subjectSummary.setFirstName(subject.getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName());
        subjectSummary.setMiddleName(subject.getMasterDefendant().getPersonDefendant().getPersonDetails().getMiddleName());
        subjectSummary.setLastName(subject.getMasterDefendant().getPersonDefendant().getPersonDetails().getLastName());
        subjectSummary.setDateOfBirth(getDateOfBirth(subject));
        subjectSummary.setAddress(setAddress(subject.getMasterDefendant().getPersonDefendant().getPersonDetails().getAddress()));
    }

    private static String getDateOfBirth(final CourtApplicationParty subject) {
        return Optional.ofNullable(subject)
                .map(CourtApplicationParty::getMasterDefendant)
                .map(MasterDefendant::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(Person::getDateOfBirth)
                .map(Object::toString)
                .orElse(null);
    }

    private Address setAddress(final uk.gov.justice.core.courts.Address address) {
        if (isNull(address)) {
            return null;
        }
        final Address newAddress = new Address();
        newAddress.setAddress1(address.getAddress1());
        newAddress.setAddress2(address.getAddress2());
        newAddress.setAddress3(address.getAddress3());
        newAddress.setAddress4(address.getAddress4());
        newAddress.setAddress5(address.getAddress5());
        newAddress.setPostCode(address.getPostcode());
        return newAddress;
    }
}
