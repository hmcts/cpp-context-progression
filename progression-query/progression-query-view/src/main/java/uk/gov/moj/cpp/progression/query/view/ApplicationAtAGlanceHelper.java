package uk.gov.moj.cpp.progression.query.view;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.courts.progression.query.AagResultPrompts.aagResultPrompts;
import static uk.gov.justice.courts.progression.query.ThirdParties.thirdParties;
import static uk.gov.justice.progression.courts.ApplicantDetails.applicantDetails;
import static uk.gov.justice.progression.courts.RespondentDetails.respondentDetails;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCivilApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.courts.progression.query.AagResultPrompts;
import uk.gov.justice.courts.progression.query.AagResults;
import uk.gov.justice.courts.progression.query.ApplicationDetails;
import uk.gov.justice.courts.progression.query.ThirdParties;
import uk.gov.justice.courts.progression.query.ThirdPartyRepresentatives;
import uk.gov.justice.progression.courts.ApplicantDetails;
import uk.gov.justice.progression.courts.RespondentDetails;
import uk.gov.justice.progression.courts.RespondentRepresentatives;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.service.OrganisationService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings({"squid:CommentedOutCodeLine"})
public class ApplicationAtAGlanceHelper {

    private static final String FIRST_LAST_NAME_FORMAT = "%s %s";
    private static final String DEFENDANTS = "defendants";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String ORGANISATION_ADDRESS = "organisationAddress";
    private static final String ADDRESS_POSTCODE = "addressPostcode";

    @Inject
    private OrganisationService organisationService;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public ApplicationDetails getApplicationDetails(final CourtApplication courtApplication) {
        final ApplicationDetails.Builder applicationBuilder = ApplicationDetails.applicationDetails()
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withApplicationParticulars(courtApplication.getApplicationParticulars());

        final CourtApplicationPayment courtApplicationPayment = courtApplication.getCourtApplicationPayment();
        if (nonNull(courtApplicationPayment)) {
            applicationBuilder.withPaymentReference(courtApplicationPayment.getPaymentReference());
        }

        final CourtCivilApplication courtCivilApplication = courtApplication.getCourtCivilApplication();
        if(nonNull(courtCivilApplication)){
            applicationBuilder.withIsCivil(courtCivilApplication.getIsCivil());
            applicationBuilder.withIsExParte(courtCivilApplication.getIsExParte());
        }
        applicationBuilder.withIsGroupCaseApplication(courtApplication.getIsGroupCaseApplication());

        final CourtApplicationType type = courtApplication.getType();
        if (nonNull(type)) {
            applicationBuilder.withApplicationType(type.getType());
            applicationBuilder.withAppeal(type.getAppealFlag());
            applicationBuilder.withApplicantAppellantFlag(type.getApplicantAppellantFlag());
        }
        ofNullable(courtApplication.getPlea()).ifPresent(applicationBuilder::withPlea);
        ofNullable(courtApplication.getVerdict()).ifPresent(applicationBuilder::withVerdict);

        ofNullable(courtApplication.getJudicialResults()).ifPresent(judicialResults -> {
            final List<AagResults> aagResultsList = judicialResults.stream().map(this::getAagResult).collect(toList());
            applicationBuilder.withAagResults(aagResultsList);
        });

        return applicationBuilder.build();
    }

    @SuppressWarnings({"squid:MethodCyclomaticComplexity"})
    public ApplicantDetails getApplicantDetails(final CourtApplication courtApplication, final JsonEnvelope envelope) {
        final ApplicantDetails.Builder applicantDetailsBuilder = applicantDetails();
        final CourtApplicationParty applicant = courtApplication.getApplicant();
        ofNullable(applicant.getProsecutingAuthority()).map(ProsecutingAuthority::getProsecutionAuthorityCode).ifPresent(applicantDetailsBuilder::withName);
        final Person person = getPersonDetails(applicant);
        final Organisation organisation = applicant.getOrganisation();
        if (nonNull(person)) {
            applicantDetailsBuilder.withName(getName(person));
            applicantDetailsBuilder.withAddress(person.getAddress());
            applicantDetailsBuilder.withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds());

            final Optional<String> representationName = getRepresentationName(applicant);
            representationName.ifPresent(applicantDetailsBuilder::withRepresentation);

            final Optional<String> remandStatus = getRemandStatus(applicant);
            remandStatus.ifPresent(applicantDetailsBuilder::withRemandStatus);
        } else if (nonNull(organisation)) {
            applicantDetailsBuilder.withName(organisation.getName());
            applicantDetailsBuilder.withAddress(organisation.getAddress());
            if (nonNull(applicant.getOrganisationPersons())) {
                applicantDetailsBuilder.withRepresentation(getOrganisationPersons(applicant.getOrganisationPersons()));
            }
        } else if (nonNull(applicant.getMasterDefendant())) {
            final Optional<PersonDefendant> personDefendantDetails = ofNullable(applicant.getMasterDefendant().getPersonDefendant());
            final Optional<LegalEntityDefendant> organisationDefendantDetails = ofNullable(applicant.getMasterDefendant().getLegalEntityDefendant());
            if (personDefendantDetails.isPresent()) {
                applicantDetailsBuilder.withName(getName(personDefendantDetails.get().getPersonDetails()));
                applicantDetailsBuilder.withAddress(personDefendantDetails.get().getPersonDetails().getAddress());
            } else if (organisationDefendantDetails.isPresent()) {
                applicantDetailsBuilder.withName(organisationDefendantDetails.get().getOrganisation().getName());
                applicantDetailsBuilder.withAddress(organisationDefendantDetails.get().getOrganisation().getAddress());
            }

            createPresentationForMasterDefendant(courtApplication, envelope, applicantDetailsBuilder, applicant);
        } else if (nonNull(applicant.getProsecutingAuthority())) {
            final ProsecutingAuthority prosecutingAuthority = applicant.getProsecutingAuthority();
            applicantDetailsBuilder.withName(prosecutingAuthority.getProsecutionAuthorityCode());
            if(nonNull(prosecutingAuthority.getAddress())) {
                applicantDetailsBuilder.withAddress(prosecutingAuthority.getAddress());
            }
        }
        applicantDetailsBuilder.withIsSubject(courtApplication.getSubject() != null && applicant.getId().equals(courtApplication.getSubject().getId()));
        if(nonNull(applicant.getUpdatedOn())){
            applicantDetailsBuilder.withUpdatedOn(applicant.getUpdatedOn());
        }
        return applicantDetailsBuilder.build();
    }

    private void createPresentationForMasterDefendant(final CourtApplication courtApplication, final JsonEnvelope envelope,
                                                      final ApplicantDetails.Builder applicantDetailsBuilder, final CourtApplicationParty applicant) {
        if(courtApplication.getSubject() != null &&
                courtApplication.getSubject().getMasterDefendant() != null &&
                applicant.getId().equals(courtApplication.getSubject().getId())){

            final UUID subjectMasterDefendantId = courtApplication.getSubject().getMasterDefendant().getMasterDefendantId();

            for(final CourtApplicationCase courtApplicationCase : courtApplication.getCourtApplicationCases()){
                final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(courtApplicationCase.getProsecutionCaseId());
                final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
                final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCasePayload, ProsecutionCase.class);
                final Optional<UUID> optionalMatchingDefendantId = prosecutionCase.getDefendants()
                        .stream()
                        .filter(defendant -> defendant.getMasterDefendantId().equals(subjectMasterDefendantId))
                        .map(Defendant::getId)
                        .findFirst();

                if(optionalMatchingDefendantId.isPresent()){
                    final JsonObject associatedCaseDefendants = organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(envelope,
                            courtApplicationCase.getProsecutionCaseId().toString());
                    final JsonArray associatedDefendants = associatedCaseDefendants.getJsonArray(DEFENDANTS);

                    final Optional<JsonObject> matchingCaseDefendant = associatedDefendants.stream().map(x -> (JsonObject) x)
                            .filter(cd -> optionalMatchingDefendantId.get().toString().equals(cd.getString(DEFENDANT_ID))).findFirst();

                    if (matchingCaseDefendant.isPresent() && nonNull(matchingCaseDefendant.get().getJsonObject(ORGANISATION_ADDRESS))) {
                        applicantDetailsBuilder.withRepresentation(createOrganisation(matchingCaseDefendant.get()));
                        break;
                    }
                }
            }
        }
    }

    private String getOrganisationPersons(final List<AssociatedPerson> associatedPersonList) {
        return associatedPersonList.stream().map(AssociatedPerson::getPerson)
                .filter(person -> (nonNull(person.getFirstName()) && !person.getFirstName().isEmpty()) && (nonNull(person.getLastName()) && !person.getLastName().isEmpty()))
                .map(person -> String.format(FIRST_LAST_NAME_FORMAT, person.getFirstName(), person.getLastName()))
                .collect(Collectors.joining(", "));

    }

    public AagResults getAagResult(final JudicialResult judicialResult) {
        final AagResults.Builder aagResultBuilder = new AagResults.Builder();
        aagResultBuilder.withId(judicialResult.getJudicialResultId());
        aagResultBuilder.withLabel(judicialResult.getLabel());
        aagResultBuilder.withOrderedDate(judicialResult.getOrderedDate());
        aagResultBuilder.withLastSharedDateTime(judicialResult.getLastSharedDateTime());
        aagResultBuilder.withAmendmentDate(judicialResult.getAmendmentDate());
        aagResultBuilder.withAmendmentReason(judicialResult.getAmendmentReason());
        aagResultBuilder.withResultText(judicialResult.getResultText());
        aagResultBuilder.withUseResultText(isUseResultText(judicialResult.getResultText()));
        ofNullable(judicialResult.getDelegatedPowers()).map(delegatedPower -> format(FIRST_LAST_NAME_FORMAT, delegatedPower.getFirstName(), delegatedPower.getLastName()))
                .ifPresent(aagResultBuilder::withAmendedBy);
        ofNullable(judicialResult.getJudicialResultPrompts()).ifPresent(judicialResultPrompts -> {
            final List<AagResultPrompts> aagResultPrompts = judicialResultPrompts.stream().map(jrp -> aagResultPrompts()
                    .withLabel(jrp.getLabel())
                    .withValue(jrp.getValue())
                    .build())
                    .collect(toList());
            aagResultBuilder.withAagResultPrompts(aagResultPrompts);
        });

        return aagResultBuilder.build();
    }

    public List<RespondentDetails> getRespondentDetails(final CourtApplication courtApplication) {
        return ofNullable(courtApplication
                .getRespondents())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(respondent -> getRespondentDetails(respondent, courtApplication.getSubject()))
                .collect(toList());
    }

    @SuppressWarnings({"squid:MethodCyclomaticComplexity"})
    private RespondentDetails getRespondentDetails(final CourtApplicationParty respondent, final CourtApplicationParty subject) {
        final RespondentDetails.Builder respondentDetailsBuilder = respondentDetails();
        ofNullable(respondent.getProsecutingAuthority()).map(ProsecutingAuthority::getProsecutionAuthorityCode).ifPresent(respondentDetailsBuilder::withName);
        respondentDetailsBuilder.withId(respondent.getId());
        final Organisation organisation = respondent.getOrganisation();
        final Person personDetails = respondent.getPersonDetails();
        if (nonNull(organisation)) {
            respondentDetailsBuilder.withName(organisation.getName());
            respondentDetailsBuilder.withAddress(organisation.getAddress());
            final List<AssociatedPerson> organisationPersons = respondent.getOrganisationPersons();
            if (nonNull(organisationPersons)) {
                respondentDetailsBuilder.withRespondentRepresentatives(getRespondentRepresentatives(organisationPersons));
            }
        } else if (nonNull(personDetails)) {
            respondentDetailsBuilder.withName(getName(personDetails));
            respondentDetailsBuilder.withAddress(personDetails.getAddress());
            final Organisation representationOrganisation = respondent.getRepresentationOrganisation();
            if (nonNull(representationOrganisation)) {
                respondentDetailsBuilder.withRespondentRepresentatives(getRespondentRepresentatives(representationOrganisation));
            }
        } else if (nonNull(respondent.getMasterDefendant())) {
            final Optional<PersonDefendant> personDefendantDetails = ofNullable(respondent.getMasterDefendant().getPersonDefendant());
            final Optional<LegalEntityDefendant> organisationDefendantDetails = ofNullable(respondent.getMasterDefendant().getLegalEntityDefendant());
            if (personDefendantDetails.isPresent()) {
                respondentDetailsBuilder.withName(getName(personDefendantDetails.get().getPersonDetails()));
                respondentDetailsBuilder.withAddress(personDefendantDetails.get().getPersonDetails().getAddress());
            } else if (organisationDefendantDetails.isPresent()) {
                respondentDetailsBuilder.withName(organisationDefendantDetails.get().getOrganisation().getName());
                respondentDetailsBuilder.withAddress(organisationDefendantDetails.get().getOrganisation().getAddress());
            }
        } else if (nonNull(respondent.getProsecutingAuthority())) {
            updateRespondentDetailsWithProsecutingAuthority(respondent, respondentDetailsBuilder);
        }
        respondentDetailsBuilder.withIsSubject(subject != null && respondent.getId().equals(subject.getId()));
        if(nonNull(respondent.getUpdatedOn())){
            respondentDetailsBuilder.withUpdatedOn(respondent.getUpdatedOn());
        }
        return respondentDetailsBuilder.build();
    }

    private void updateRespondentDetailsWithProsecutingAuthority(CourtApplicationParty respondent, RespondentDetails.Builder respondentDetailsBuilder) {
        final ProsecutingAuthority prosecutingAuthority = respondent.getProsecutingAuthority();
        respondentDetailsBuilder.withName(prosecutingAuthority.getProsecutionAuthorityCode());
        if(nonNull(prosecutingAuthority.getAddress())) {
            respondentDetailsBuilder.withAddress(prosecutingAuthority.getAddress());
        }
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
        return ofNullable(applicant.getMasterDefendant())
                .map(MasterDefendant::getPersonDefendant)
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
        return ofNullable(applicant.getMasterDefendant())
                .map(MasterDefendant::getPersonDefendant)
                .map(PersonDefendant::getBailStatus)
                .map(BailStatus::getDescription);
    }

    private Optional<String> getRepresentationName(final CourtApplicationParty applicant) {
        return ofNullable(applicant.getRepresentationOrganisation())
                .map(Organisation::getName);
    }

    public List<ThirdParties> getThirdPartyDetails(final CourtApplication courtApplication) {
        return ofNullable(courtApplication
                .getThirdParties())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(this::getThirdPartyDetails)
                .collect(toList());
    }

    private ThirdParties getThirdPartyDetails(final CourtApplicationParty courtApplicationParty) {
        final ThirdParties.Builder thirdPartiesDetailsBuilder = thirdParties();
        thirdPartiesDetailsBuilder.withId(courtApplicationParty.getId());

        ofNullable(courtApplicationParty.getOrganisation()).ifPresent(organisation -> {
            thirdPartiesDetailsBuilder.withName(organisation.getName());
            thirdPartiesDetailsBuilder.withAddress(organisation.getAddress());
            final List<AssociatedPerson> organisationPersons = courtApplicationParty.getOrganisationPersons();
            if (nonNull(organisationPersons)) {
                thirdPartiesDetailsBuilder.withThirdPartyRepresentatives(getThirdPartyRepresentatives(organisationPersons));
            }
        });

        ofNullable(courtApplicationParty.getPersonDetails()).ifPresent(personDetails -> {
            thirdPartiesDetailsBuilder.withName(getName(personDetails));
            thirdPartiesDetailsBuilder.withAddress(personDetails.getAddress());
            final Organisation representationOrganisation = courtApplicationParty.getRepresentationOrganisation();
            if (nonNull(representationOrganisation)) {
                thirdPartiesDetailsBuilder.withThirdPartyRepresentatives(getThirdPartyRepresentatives(representationOrganisation));
            }
        });

        ofNullable(courtApplicationParty.getMasterDefendant()).ifPresent(masterDefendant -> {
            final Optional<PersonDefendant> personDefendantDetails = ofNullable(masterDefendant.getPersonDefendant());
            final Optional<LegalEntityDefendant> organisationDefendantDetails = ofNullable(masterDefendant.getLegalEntityDefendant());
            if (personDefendantDetails.isPresent()) {
                thirdPartiesDetailsBuilder.withName(getName(personDefendantDetails.get().getPersonDetails()));
                thirdPartiesDetailsBuilder.withAddress(personDefendantDetails.get().getPersonDetails().getAddress());
            } else if (organisationDefendantDetails.isPresent()) {
                thirdPartiesDetailsBuilder.withName(organisationDefendantDetails.get().getOrganisation().getName());
                thirdPartiesDetailsBuilder.withAddress(organisationDefendantDetails.get().getOrganisation().getAddress());
            }
        });

        ofNullable(courtApplicationParty.getProsecutingAuthority()).ifPresent(prosecutingAuthority -> {
            thirdPartiesDetailsBuilder.withName(prosecutingAuthority.getName());
            thirdPartiesDetailsBuilder.withAddress(prosecutingAuthority.getAddress());
        });

        return thirdPartiesDetailsBuilder.build();
    }

    private List<ThirdPartyRepresentatives> getThirdPartyRepresentatives(final List<AssociatedPerson> organisationPersons) {
        return organisationPersons
                .stream()
                .map(p -> new ThirdPartyRepresentatives(getName(p.getPerson()), p.getRole()))
                .collect(toList());
    }

    private List<ThirdPartyRepresentatives> getThirdPartyRepresentatives(final Organisation representationOrganisation) {
        final ThirdPartyRepresentatives respondentRepresentatives = ThirdPartyRepresentatives.thirdPartyRepresentatives()
                .withRepresentativeName(representationOrganisation.getName())
                .build();
        final List<ThirdPartyRepresentatives> thirdPartyRepresentatives = new ArrayList<>();
        thirdPartyRepresentatives.add(respondentRepresentatives);
        return thirdPartyRepresentatives;
    }

    private boolean isUseResultText(final String resultText){
        final String checkValue = Arrays.stream(resultText.split(" ")).limit(3).collect(Collectors.joining(" "));
        final Pattern pattern = Pattern.compile("\\w - \\w", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(checkValue);
        return matcher.find();
    }

    private String createOrganisation(final JsonObject completeOrganisationDetails) {
        final JsonObject address = completeOrganisationDetails.getJsonObject(ORGANISATION_ADDRESS);

        final String address1 = address.containsKey("address1") ? address.getString("address1") : " ";
        final String address2 = address.containsKey("address2") ? address.getString("address2") : " ";
        final String address3 = address.containsKey("address3") ? address.getString("address3") : " ";
        final String address4 = address.containsKey("address4") ? address.getString("address4") : " ";
        final String postCode = address.containsKey(ADDRESS_POSTCODE) ? address.getString(ADDRESS_POSTCODE) : " ";

        return completeOrganisationDetails.getString("organisationName") + ',' +
                address1 + ',' +
                address2 + ',' +
                address3 + ',' +
                address4 + ',' +
                postCode;
    }
}
