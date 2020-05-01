package uk.gov.moj.cpp.progression.query.view;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.Period.between;
import static java.util.Objects.nonNull;
import static uk.gov.justice.progression.courts.LegalEntityDefendant.legalEntityDefendant;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.CaagDefendantOffences;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.CaagResultPrompts;
import uk.gov.justice.progression.courts.CaagResults;
import uk.gov.justice.progression.courts.CaseDetails;
import uk.gov.justice.progression.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.ProsecutorDetails;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

public class CaseAtAGlanceHelper {

    static final String ADDRESS_1 = "address1";
    static final String ADDRESS_2 = "address2";
    static final String ADDRESS_3 = "address3";
    static final String ADDRESS_4 = "address4";
    static final String ADDRESS_5 = "address5";
    static final String POSTCODE = "postcode";
    static final String YOUTH_MARKER_TYPE = "Youth";

    private final ProsecutionCase prosecutionCase;
    private final List<Hearings> caseHearings;
    private final ReferenceDataService referenceDataService;

    public CaseAtAGlanceHelper(final ProsecutionCase prosecutionCase, List<Hearings> caseHearings, final ReferenceDataService referenceDataService) {
        this.prosecutionCase = prosecutionCase;
        this.caseHearings = caseHearings;
        this.referenceDataService = referenceDataService;
    }

    public CaseDetails getCaseDetails() {

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();

        final CaseDetails.Builder caseDetailsBuilder = CaseDetails.caseDetails();
        if (nonNull(prosecutionCaseIdentifier) && nonNull(prosecutionCaseIdentifier.getCaseURN())) {
            caseDetailsBuilder.withCaseURN(prosecutionCaseIdentifier.getCaseURN());
        }

        if (nonNull(prosecutionCase.getCaseMarkers()) && !prosecutionCase.getCaseMarkers().isEmpty()) {
            caseDetailsBuilder.withCaseMarkers(prosecutionCase.getCaseMarkers().stream().map(Marker::getMarkerTypeDescription).collect(Collectors.toList()));
        }

        caseDetailsBuilder.withCaseStatus(prosecutionCase.getCaseStatus());
        caseDetailsBuilder.withRemovalReason(prosecutionCase.getRemovalReason());

        return caseDetailsBuilder.build();
    }

    public ProsecutorDetails getProsecutorDetails() {
        final ProsecutorDetails.Builder prosecutorDetailsBuilder = ProsecutorDetails.prosecutorDetails();

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        if (nonNull(prosecutionCaseIdentifier)) {
            prosecutorDetailsBuilder.withProsecutionAuthorityReference(prosecutionCaseIdentifier.getProsecutionAuthorityReference());
            prosecutorDetailsBuilder.withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode());

            final UUID prosecutionAuthorityId = prosecutionCaseIdentifier.getProsecutionAuthorityId();
            prosecutorDetailsBuilder.withProsecutionAuthorityId(prosecutionAuthorityId);
            prosecutorDetailsBuilder.withAddress(getProsecutorAddress(prosecutionAuthorityId));
        }

        return prosecutorDetailsBuilder.build();
    }

    public List<CaagDefendants> getDefendantsWithOffenceDetails() {
        final List<CaagDefendants> defendantList = new ArrayList<>();

        if (nonNull(prosecutionCase.getDefendants())) {

            prosecutionCase.getDefendants().forEach(defendant -> {
                final CaagDefendants.Builder caagDefendant = CaagDefendants.caagDefendants().withId(defendant.getId());
                setDefendantPersonalDetails(defendant, caagDefendant);

                if (nonNull(defendant.getOffences())) {
                    final List<CaagDefendantOffences> caagDefendantOffenceList = new ArrayList<>();

                    defendant.getOffences().forEach(offence -> {
                        final CaagDefendantOffences.Builder caagDefendantOffenceBuilder = CaagDefendantOffences.caagDefendantOffences();
                        setDefendantOffenceDetails(defendant.getId(), offence, caagDefendantOffenceBuilder);
                        caagDefendantOffenceList.add(caagDefendantOffenceBuilder.build());
                    });

                    caagDefendant.withCaagDefendantOffences(caagDefendantOffenceList);
                    caagDefendant.withLegalAidStatus(defendant.getLegalAidStatus());
                }

                defendantList.add(caagDefendant.build());
            });
        }

        return defendantList;
    }

    private void setDefendantOffenceDetails(final UUID defendantId, final Offence offence, final CaagDefendantOffences.Builder caagDefendantOffenceBuilder) {

        caagDefendantOffenceBuilder.withId(offence.getId());
        caagDefendantOffenceBuilder.withOffenceCode(offence.getOffenceCode());
        caagDefendantOffenceBuilder.withCount(offence.getCount());

        caagDefendantOffenceBuilder.withOffenceTitle(offence.getOffenceTitle());
        caagDefendantOffenceBuilder.withOffenceTitleWelsh(offence.getOffenceTitleWelsh());
        caagDefendantOffenceBuilder.withWording(offence.getWording());
        caagDefendantOffenceBuilder.withWordingWelsh(offence.getWordingWelsh());
        caagDefendantOffenceBuilder.withOffenceLegislation(offence.getOffenceLegislation());
        caagDefendantOffenceBuilder.withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh());

        caagDefendantOffenceBuilder.withStartDate(offence.getStartDate());
        caagDefendantOffenceBuilder.withEndDate(offence.getEndDate());

        caagDefendantOffenceBuilder.withAllocationDecision(offence.getAllocationDecision());
        caagDefendantOffenceBuilder.withCustodyTimeLimit(offence.getCustodyTimeLimit());

        if (nonNull(offence.getJudicialResults())) {
            caagDefendantOffenceBuilder.withCaagResults(extractResults(getResultsFromAllHearings(defendantId, offence.getId())));
        }
        caagDefendantOffenceBuilder.withPlea(offence.getPlea());
        caagDefendantOffenceBuilder.withVerdict(offence.getVerdict());
    }

    private List<CaagResults> extractResults(final List<JudicialResult> judicialResults) {
        return judicialResults.stream()
                .filter(jr -> !Boolean.TRUE.equals(jr.getIsDeleted()))
                .map(jr -> {
                    final CaagResults.Builder caagResultsBuilder = CaagResults.caagResults()
                            .withId(jr.getJudicialResultId())
                            .withLabel(jr.getLabel())
                            .withOrderedDate(jr.getOrderedDate())
                            .withLastSharedDateTime(jr.getLastSharedDateTime())
                            .withAmendmentReason(jr.getAmendmentReason())
                            .withAmendmentDate(jr.getAmendmentDate());

                    if (nonNull(jr.getJudicialResultPrompts())) {
                        caagResultsBuilder.withCaagResultPrompts(extractResultPrompts(jr.getJudicialResultPrompts()));
                    }
                    if (nonNull(jr.getDelegatedPowers())) {
                        caagResultsBuilder.withAmendedBy(format("%s %s", jr.getDelegatedPowers().getFirstName(), jr.getDelegatedPowers().getLastName()));
                    }
                    return caagResultsBuilder.build();
                })
                .sorted(Comparator.comparing(CaagResults::getOrderedDate).reversed())
                .collect(Collectors.toList());
    }

    private List<CaagResultPrompts> extractResultPrompts(final List<JudicialResultPrompt> judicialResultPrompts) {
        return judicialResultPrompts.stream()
                .map(jrp -> CaagResultPrompts.caagResultPrompts()
                        .withLabel(jrp.getLabel())
                        .withValue(jrp.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private void setDefendantPersonalDetails(final Defendant defendant, final CaagDefendants.Builder caagDefendant) {

        final List<String> defendantMarkers = new ArrayList<>();
        if (Boolean.TRUE.equals(defendant.getIsYouth())) {
            defendantMarkers.add(YOUTH_MARKER_TYPE);
        }

        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            caagDefendant.withFirstName(defendant.getPersonDefendant().getPersonDetails().getFirstName())
                    .withLastName(defendant.getPersonDefendant().getPersonDetails().getLastName())
                    .withDateOfBirth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())
                    .withAge(calculateAge(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()))
                    .withAddress(defendant.getPersonDefendant().getPersonDetails().getAddress())
                    .withInterpreterLanguageNeeds(defendant.getPersonDefendant().getPersonDetails().getInterpreterLanguageNeeds())
                    .withNationality(getNationalityDescription(defendant.getPersonDefendant().getPersonDetails()));

            if (nonNull(defendant.getPersonDefendant().getBailStatus())) {
                caagDefendant.withRemandStatus(defendant.getPersonDefendant().getBailStatus().getDescription());
            }

            if (nonNull(defendant.getPersonDefendant().getPersonDetails().getPersonMarkers())) {
                defendantMarkers.addAll(defendant.getPersonDefendant().getPersonDetails().getPersonMarkers().stream()
                        .map(Marker::getMarkerTypeDescription)
                        .collect(Collectors.toList()));
            }
        }

        if (!defendantMarkers.isEmpty()) {
            caagDefendant.withDefendantMarkers(defendantMarkers);
        }

        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation())) {
            caagDefendant.withLegalEntityDefendant(legalEntityDefendant()
                    .withName(defendant.getLegalEntityDefendant().getOrganisation().getName())
                    .withAddress(defendant.getLegalEntityDefendant().getOrganisation().getAddress())
                    .build());
        }
    }

    private String getNationalityDescription(final Person person) {
        return nonNull(person.getNationalityDescription()) && nonNull(person.getAdditionalNationalityDescription())
                ? format("%s, %s", person.getNationalityDescription(), person.getAdditionalNationalityDescription())
                : person.getNationalityDescription();
    }

    static Integer calculateAge(final LocalDate dateOfBirth) {
        return nonNull(dateOfBirth) ? between(dateOfBirth, now()).getYears() : null;
    }

    private Address getProsecutorAddress(final UUID prosecutionAuthorityId) {
        final Optional<JsonObject> prosecutorJson = referenceDataService.getProsecutor(prosecutionAuthorityId.toString());

        if (prosecutorJson.isPresent() && nonNull(prosecutorJson.get().getJsonObject("address"))) {
            final JsonObject addressJson = prosecutorJson.get().getJsonObject("address");
            return Address.address()
                    .withAddress1(addressJson.getString(ADDRESS_1, null))
                    .withAddress2(addressJson.getString(ADDRESS_2, null))
                    .withAddress3(addressJson.getString(ADDRESS_3, null))
                    .withAddress4(addressJson.getString(ADDRESS_4, null))
                    .withAddress5(addressJson.getString(ADDRESS_5, null))
                    .withPostcode(addressJson.getString(POSTCODE, null))
                    .build();
        }
        return null;
    }

    private List<JudicialResult> getResultsFromAllHearings(final UUID defendantId, final UUID offenceId) {
        final List<JudicialResult> defendantJRs = new ArrayList<>();

        caseHearings.stream()
                .filter(hearings -> hearings.getHearingListingStatus() == HearingListingStatus.HEARING_RESULTED)
                .forEach(hearings -> hearings.getDefendants().stream()
                        .filter(d -> d.getId().equals(defendantId))

                        .forEach(d -> d.getOffences().stream()
                                .filter(o -> o.getId().equals(offenceId))
                                .forEach(o -> defendantJRs.addAll(o.getJudicialResults()))));

        return defendantJRs;
    }

}
