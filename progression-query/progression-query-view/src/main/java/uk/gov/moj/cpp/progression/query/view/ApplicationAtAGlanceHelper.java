package uk.gov.moj.cpp.progression.query.view;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.progression.courts.ApplicantDetails.applicantDetails;
import static uk.gov.justice.progression.courts.ApplicationDetails.Builder;
import static uk.gov.justice.progression.courts.ApplicationDetails.applicationDetails;
import static uk.gov.justice.progression.courts.RespondentDetails.respondentDetails;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.courts.ApplicationDetails;
import uk.gov.justice.progression.courts.RespondentDetails;
import uk.gov.justice.progression.courts.RespondentRepresentatives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicationAtAGlanceHelper {

    public ApplicationDetails getApplicationDetails(final CourtApplication courtApplication) {
        final Builder applicationBuilder = applicationDetails()
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withApplicationParticulars(courtApplication.getApplicationParticulars());

        final CourtApplicationPayment courtApplicationPayment = courtApplication.getCourtApplicationPayment();
        if (nonNull(courtApplicationPayment)) {
            applicationBuilder.withFeePayable(FALSE.equals(courtApplicationPayment.getIsFeeExempt()))
                    .withPaymentReference(courtApplicationPayment.getPaymentReference());
        }

        final CourtApplicationType type = courtApplication.getType();
        if (nonNull(type)) {
            applicationBuilder.withApplicationType(type.getApplicationType());
            applicationBuilder.withAppeal(type.getIsAppealApplication());
        }

        return applicationBuilder.build();
    }

    public ApplicantDetails getApplicantDetails(final CourtApplication courtApplication) {
        final ApplicantDetails.Builder applicantDetailsBuilder = applicantDetails();
        final CourtApplicationParty applicant = courtApplication.getApplicant();
        final Person person = getPersonDetails(applicant);
        final Organisation organisation = applicant.getOrganisation();

        final CourtApplicationType type = courtApplication.getType();
        if (nonNull(type)) {
            applicantDetailsBuilder.withApplicantSynonym(type.getApplicantSynonym());
        }

        if (nonNull(person)) {
            applicantDetailsBuilder.withName(getName(person));
            applicantDetailsBuilder.withAddress(person.getAddress());
            applicantDetailsBuilder.withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds());

            final Optional<String> representationName = getRepresentationName(applicant);
            if (representationName.isPresent()) {
                applicantDetailsBuilder.withRepresentation(representationName.get());
            }

            final Optional<String> remandStatus = getRemandStatus(applicant);
            if (remandStatus.isPresent()) {
                applicantDetailsBuilder.withRemandStatus(remandStatus.get());
            }
        } else if (nonNull(organisation)) {
            applicantDetailsBuilder.withName(organisation.getName());
            applicantDetailsBuilder.withAddress(organisation.getAddress());
            if (nonNull(applicant.getOrganisationPersons())) {
                applicantDetailsBuilder.withRepresentation(getOrganisationPersons(applicant.getOrganisationPersons()));
            }
        }
        return applicantDetailsBuilder.build();
    }

    private String getOrganisationPersons(final List<AssociatedPerson> associatedPersonList) {
        return associatedPersonList.stream().map(AssociatedPerson::getPerson)
                .filter(person -> (nonNull(person.getFirstName()) && !person.getFirstName().isEmpty()) && (nonNull(person.getLastName()) && !person.getLastName().isEmpty()))
                .map(person -> String.format("%s %s", person.getFirstName(), person.getLastName()))
                .collect(Collectors.joining(", "));

    }

    public List<RespondentDetails> getRespondentDetails(final CourtApplication courtApplication) {
        return ofNullable(courtApplication
                .getRespondents())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(this::getRespondentDetails)
                .collect(toList());
    }

    private RespondentDetails getRespondentDetails(final CourtApplicationRespondent courtApplicationRespondent) {
        final RespondentDetails.Builder respondentDetailsBuilder = respondentDetails();
        final CourtApplicationParty courtApplicationParty = courtApplicationRespondent.getPartyDetails();

        final Organisation organisation = courtApplicationParty.getOrganisation();
        final Person personDetails = courtApplicationParty.getPersonDetails();
        if (nonNull(organisation)) {
            respondentDetailsBuilder.withName(organisation.getName());
            respondentDetailsBuilder.withAddress(organisation.getAddress());
            final List<AssociatedPerson> organisationPersons = courtApplicationParty.getOrganisationPersons();
            if (nonNull(organisationPersons)) {
                respondentDetailsBuilder.withRespondentRepresentatives(getRespondentRepresentatives(organisationPersons));
            }
        } else if (nonNull(personDetails)) {
            respondentDetailsBuilder.withName(getName(personDetails));
            respondentDetailsBuilder.withAddress(personDetails.getAddress());
            final Organisation representationOrganisation = courtApplicationParty.getRepresentationOrganisation();
            if (nonNull(representationOrganisation)) {
                respondentDetailsBuilder.withRespondentRepresentatives(getRespondentRepresentatives(representationOrganisation));
            }
        }

        return respondentDetailsBuilder.build();
    }

    private List<RespondentRepresentatives> getRespondentRepresentatives(final Organisation representationOrganisation) {
        final RespondentRepresentatives respondentRepresentatives = RespondentRepresentatives.respondentRepresentatives()
                .withRepresentativeName(representationOrganisation.getName())
                .build();
        final List<RespondentRepresentatives> respondentRepresentativesList = new ArrayList<>();
        respondentRepresentativesList.add(respondentRepresentatives);
        return respondentRepresentativesList;
    }

    private List<RespondentRepresentatives> getRespondentRepresentatives(final List<AssociatedPerson> organisationPersons) {
        return organisationPersons
                .stream()
                .map(p -> new RespondentRepresentatives(getName(p.getPerson()), p.getRole()))
                .collect(toList());
    }

    private Person getPersonDetails(final CourtApplicationParty applicant) {
        return ofNullable(applicant.getDefendant())
                .map(Defendant::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .orElse(applicant.getPersonDetails());
    }

    private String getName(final Person person) {
        final String string = Stream.of(person.getFirstName(), person.getLastName())
                .filter(s -> s != null && !s.isEmpty())
                .collect(joining(" "));

        return string.isEmpty() ? null : string;
    }

    private Optional<String> getRemandStatus(final CourtApplicationParty applicant) {
        return ofNullable(applicant.getDefendant())
                .map(Defendant::getPersonDefendant)
                .map(PersonDefendant::getBailStatus)
                .map(BailStatus::getDescription);
    }

    private Optional<String> getRepresentationName(final CourtApplicationParty applicant) {
        return ofNullable(applicant.getRepresentationOrganisation())
                .map(Organisation::getName);
    }
}
