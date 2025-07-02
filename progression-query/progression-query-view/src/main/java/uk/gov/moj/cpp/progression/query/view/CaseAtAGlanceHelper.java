package uk.gov.moj.cpp.progression.query.view;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.Period.between;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_RESULTED;
import static uk.gov.justice.progression.courts.CaagDefendants.caagDefendants;
import static uk.gov.justice.progression.courts.LegalEntityDefendant.legalEntityDefendant;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.progression.courts.CaagDefendantOffences;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.CaagDefendants.Builder;
import uk.gov.justice.progression.courts.CaagResultPrompts;
import uk.gov.justice.progression.courts.CaagResults;
import uk.gov.justice.progression.courts.CaseDetails;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.ProsecutorDetails;
import uk.gov.justice.progression.query.RelatedReference;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

public class CaseAtAGlanceHelper {

    static final String ADDRESS_1 = "address1";
    static final String ADDRESS_2 = "address2";
    static final String ADDRESS_3 = "address3";
    static final String ADDRESS_4 = "address4";
    static final String ADDRESS_5 = "address5";
    static final String POSTCODE = "postcode";
    static final String YOUTH_MARKER_TYPE = "Youth";

    private final ProsecutionCase prosecutionCase;
    private final List<Hearings> hearingsList;
    private final ReferenceDataService referenceDataService;
    private final CivilFeeRepository civilFeeRepository;
    private final RelatedReferenceRepository relatedReferenceRepository;

    public CaseAtAGlanceHelper(final ProsecutionCase prosecutionCase, final List<Hearings> hearingsList, final ReferenceDataService referenceDataService, final CivilFeeRepository civilFeeRepository, final RelatedReferenceRepository relatedReferenceRepository) {
        this.prosecutionCase = prosecutionCase;
        this.hearingsList = new ArrayList<>(hearingsList);
        this.referenceDataService = referenceDataService;
        this.civilFeeRepository = civilFeeRepository;
        this.relatedReferenceRepository = relatedReferenceRepository;
    }

    static Integer getAge(final LocalDate dateOfBirth) {
        return nonNull(dateOfBirth) ? between(dateOfBirth, now()).getYears() : null;
    }

    public CaseDetails getCaseDetails() {
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final CaseDetails.Builder caseDetailsBuilder = CaseDetails.caseDetails();

        if (nonNull(prosecutionCaseIdentifier) && nonNull(prosecutionCaseIdentifier.getCaseURN())) {
            caseDetailsBuilder.withCaseURN(prosecutionCaseIdentifier.getCaseURN());
        }
        if (nonNull(prosecutionCase.getCaseMarkers()) && !prosecutionCase.getCaseMarkers().isEmpty()) {
            caseDetailsBuilder.withCaseMarkers(prosecutionCase.getCaseMarkers().stream().map(Marker::getMarkerTypeDescription).collect(toList()));
        }

        caseDetailsBuilder.withCaseStatus(prosecutionCase.getCaseStatus());
        caseDetailsBuilder.withRemovalReason(prosecutionCase.getRemovalReason());
        final List<RelatedReference> relatedReferences = getRelatedReferences(prosecutionCase.getId());

        if(CollectionUtils.isNotEmpty(relatedReferences)) {
            caseDetailsBuilder.withRelatedReferenceList(relatedReferences);
        }

        caseDetailsBuilder.withMigrationSourceSystem(prosecutionCase.getMigrationSourceSystem());

        if (prosecutionCase.getInitiationCode() != null) {
            caseDetailsBuilder.withInitiationCode(
                    prosecutionCase.getInitiationCode().toString());
        }
        if(nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil()){
            getCivilDetails(prosecutionCase, caseDetailsBuilder);
        }

        return caseDetailsBuilder.build();
    }
    private List<RelatedReference> getRelatedReferences(final UUID caseId) {
        return relatedReferenceRepository
                .findByProsecutionCaseId(caseId)
                .stream()
                .map(e -> RelatedReference.relatedReference()
                        .withRelatedReference(e.getReference())
                        .withRelatedReferenceId(e.getId())
                        .withProsecutionCaseId(e.getProsecutionCaseId())
                        .build())
                .collect(toList());
    }

    private void getCivilDetails(ProsecutionCase prosecutionCase, CaseDetails.Builder caseDetailsBuilder) {
        caseDetailsBuilder.withIsCivil(prosecutionCase.getIsCivil());

        if(isNotEmpty(prosecutionCase.getCivilFees())){
            caseDetailsBuilder.withCivilFees(getCivilFeeEntity(prosecutionCase));
        }

        if(nonNull(prosecutionCase.getIsGroupMaster())){
            caseDetailsBuilder.withIsGroupMaster(prosecutionCase.getIsGroupMaster());
        }
        if(nonNull(prosecutionCase.getGroupId())){
            caseDetailsBuilder.withGroupId(prosecutionCase.getGroupId());
        }
        if(nonNull(prosecutionCase.getIsGroupMember())){
            caseDetailsBuilder.withIsGroupMember(prosecutionCase.getIsGroupMember());
        }
    }

    public List<CivilFees> getCivilFeeEntity(ProsecutionCase prosecutionCase){
        final List<CivilFees> civilFeesList = new ArrayList<>();

        if(nonNull(prosecutionCase.getCivilFees())){
            prosecutionCase.getCivilFees().forEach(civilFee -> {
                final CivilFeeEntity civilFeeEntity = civilFeeRepository.findBy(civilFee.getFeeId());
                civilFeesList.add(CivilFees.civilFees()
                        .withFeeId(civilFeeEntity.getFeeId())
                        .withFeeType(FeeType.valueOf(civilFeeEntity.getFeeType().name()))
                        .withFeeStatus(FeeStatus.valueOf(civilFeeEntity.getFeeStatus().name()))
                        .withPaymentReference(civilFeeEntity.getPaymentReference())
                        .build());
            });
        }

        return civilFeesList;
    }

    public ProsecutorDetails getProsecutorDetails() {
        final ProsecutorDetails.Builder prosecutorDetailsBuilder = ProsecutorDetails.prosecutorDetails();
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Prosecutor prosecutor = prosecutionCase.getProsecutor();
        if (nonNull(prosecutor)) {
            prosecutorDetailsBuilder.withProsecutionAuthorityCode(prosecutor.getProsecutorCode());
            if (nonNull(prosecutionCaseIdentifier)) {
                prosecutorDetailsBuilder.withProsecutionAuthorityReference(prosecutionCaseIdentifier.getProsecutionAuthorityReference());
            }
            final UUID prosecutionAuthorityId = prosecutor.getProsecutorId();
            prosecutorDetailsBuilder.withProsecutionAuthorityId(prosecutionAuthorityId);
            prosecutorDetailsBuilder.withAddress(getProsecutorAddress(prosecutionAuthorityId));

        } else if (nonNull(prosecutionCaseIdentifier)) {
            prosecutorDetailsBuilder.withProsecutionAuthorityReference(prosecutionCaseIdentifier.getProsecutionAuthorityReference());
            prosecutorDetailsBuilder.withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode());

            final UUID prosecutionAuthorityId = prosecutionCaseIdentifier.getProsecutionAuthorityId();
            prosecutorDetailsBuilder.withProsecutionAuthorityId(prosecutionAuthorityId);
            prosecutorDetailsBuilder.withAddress(isNameInformationEmpty(prosecutionCaseIdentifier) ? getProsecutorAddress(prosecutionAuthorityId) : prosecutionCaseIdentifier.getAddress());
        }

        prosecutorDetailsBuilder.withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError());

        return prosecutorDetailsBuilder.build();
    }

    private boolean isNameInformationEmpty(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isBlank(prosecutionCaseIdentifier.getProsecutionAuthorityName());
    }

    public List<CaagDefendants> getCaagDefendantsList(final Map<UUID, LocalDate> defendantUpdatedOn) {

        final List<CaagDefendants> caagDefendantsList = new ArrayList<>();
        final List<Defendant> defendantList = prosecutionCase.getDefendants();

        for (final Defendant defendant : defendantList) {
            final Builder caagDefendantBuilder = caagDefendants().withMasterDefendantId(defendant.getMasterDefendantId());
            setDefendantPersonalDetails(defendant, caagDefendantBuilder);
            final List<CaagDefendantOffences> caagDefendantOffencesList = getCaagDefendantOffencesList(defendant);
            final List<JudicialResult> defendantJudicialResultList = getDefendantLevelJudicialResults(defendant.getMasterDefendantId());
            final List<JudicialResult> defendantCaseJudicialResultList = getCaseLevelJudicialResults(defendant.getId());

            if (!isEmpty(caagDefendantOffencesList)) {
                caagDefendantBuilder.withCaagDefendantOffences(caagDefendantOffencesList);
            }
            if (!isEmpty(defendantCaseJudicialResultList)) {
                caagDefendantBuilder.withDefendantCaseJudicialResults(defendantCaseJudicialResultList);
            }
            if (!isEmpty(defendantJudicialResultList)) {
                caagDefendantBuilder.withDefendantJudicialResults(defendantJudicialResultList);
            }
            caagDefendantBuilder.withLegalAidStatus(defendant.getLegalAidStatus());
            if(nonNull(defendantUpdatedOn) && nonNull(defendantUpdatedOn.get(defendant.getId()))) {
                caagDefendantBuilder.withUpdatedOn(defendantUpdatedOn.get(defendant.getId()));
            }
            setCtlExpiryDate(defendant, caagDefendantBuilder);

            caagDefendantsList.add(caagDefendantBuilder.build());
        }
        return caagDefendantsList;
    }

    private void setCtlExpiryDate(final Defendant defendant, final Builder caagDefendantBuilder) {
        final LocalDate ctlExpiryDate = ofNullable(defendant.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(offence -> nonNull(offence.getCustodyTimeLimit()) && nonNull(offence.getCustodyTimeLimit().getTimeLimit()))
                .map(offence -> offence.getCustodyTimeLimit().getTimeLimit())
                .min(LocalDate::compareTo)
                .orElse(null);

        if (nonNull(ctlExpiryDate)) {
            caagDefendantBuilder.withCtlExpiryDate(ctlExpiryDate);
            caagDefendantBuilder.withCtlExpiryCountDown((int) DAYS.between(LocalDate.now(), ctlExpiryDate));
        }
    }

    private List<CaagDefendantOffences> getCaagDefendantOffencesList(final Defendant defendant) {
        final List<CaagDefendantOffences> caagDefendantOffenceList = new ArrayList<>();
        final List<Offence> offenceList = defendant.getOffences();

        if (nonNull(offenceList)) {
            for (final Offence offence : offenceList) {
                final CaagDefendantOffences.Builder caagDefendantOffenceBuilder = CaagDefendantOffences.caagDefendantOffences();
                final List<JudicialResult> resultsFromAllHearings = getResultsFromAllHearings(defendant.getId(), offence.getId());
                final List<CaagResults> caagResultsList = extractResults(resultsFromAllHearings);
                final Optional<Plea> plea = getPlea(defendant.getId(), offence);
                final Optional<IndicatedPlea> indicatedPlea = getIndicatedPlea(defendant.getId(), offence);
                final Optional<Verdict> verdict = getVerdict(defendant.getId(), offence);

                caagDefendantOffenceBuilder.withCaagResults(caagResultsList);
                caagDefendantOffenceBuilder.withId(offence.getId());
                caagDefendantOffenceBuilder.withOffenceCode(offence.getOffenceCode());
                caagDefendantOffenceBuilder.withCount(offence.getCount());
                caagDefendantOffenceBuilder.withIndictmentParticular(offence.getIndictmentParticular());
                caagDefendantOffenceBuilder.withOrderIndex(offence.getOrderIndex());
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
                plea.ifPresent(caagDefendantOffenceBuilder::withPlea);
                indicatedPlea.ifPresent(caagDefendantOffenceBuilder::withIndicatedPlea);
                verdict.ifPresent(caagDefendantOffenceBuilder::withVerdict);

                if (CollectionUtils.isNotEmpty(offence.getReportingRestrictions())) {
                    caagDefendantOffenceBuilder.withReportingRestrictions(offence.getReportingRestrictions());
                }
                caagDefendantOffenceList.add(caagDefendantOffenceBuilder.build());
            }
        }

        return caagDefendantOffenceList;
    }

    private List<CaagResults> extractResults(final List<JudicialResult> judicialResults) {
        return judicialResults.stream()
                .filter(jr -> !ofNullable(jr.getIsDeleted()).orElse(false))
                .map(jr -> {
                    final CaagResults.Builder caagResultsBuilder = CaagResults.caagResults()
                            .withId(jr.getJudicialResultId())
                            .withLabel(jr.getLabel())
                            .withUsergroups(jr.getUsergroups())
                            .withOrderedDate(jr.getOrderedDate())
                            .withLastSharedDateTime(jr.getLastSharedDateTime())
                            .withAmendmentReason(jr.getAmendmentReason())
                            .withAmendmentDate(jr.getAmendmentDate());

                    setResultText(jr, caagResultsBuilder);

                    if (nonNull(jr.getJudicialResultPrompts())) {
                        caagResultsBuilder.withCaagResultPrompts(extractResultPrompts(jr.getJudicialResultPrompts()));
                    }
                    if (nonNull(jr.getDelegatedPowers())) {
                        caagResultsBuilder.withAmendedBy(format("%s %s", jr.getDelegatedPowers().getFirstName(), jr.getDelegatedPowers().getLastName()));
                    }
                    return caagResultsBuilder.build();
                })
                .sorted(comparing(CaagResults::getOrderedDate).reversed())
                .collect(toList());
    }

    private void setResultText(final JudicialResult jr, final CaagResults.Builder caagResultsBuilder) {
        if (nonNull(jr.getResultText())) {
            caagResultsBuilder.withResultText(jr.getResultText());
            caagResultsBuilder.withUseResultText(isUseResultText(jr.getResultText()));
        }
    }

    private List<CaagResultPrompts> extractResultPrompts(final List<JudicialResultPrompt> judicialResultPrompts) {
        return judicialResultPrompts.stream()
                .map(jrp -> CaagResultPrompts.caagResultPrompts()
                        .withLabel(jrp.getLabel())
                        .withValue(jrp.getValue())
                        .withUsergroups(jrp.getUsergroups())
                        .build())
                .collect(toList());
    }

    private void setDefendantPersonalDetails(final Defendant defendant, final Builder caagDefendantBuilder) {
        final List<String> defendantMarkers = new ArrayList<>();
        final PersonDefendant personDefendant = defendant.getPersonDefendant();

        if (ofNullable(defendant.getIsYouth()).orElse(false)) {
            defendantMarkers.add(YOUTH_MARKER_TYPE);
        }

        caagDefendantBuilder.withId(defendant.getId())
                .withPncId(defendant.getPncId());

        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails())) {
            final Person personDetails = personDefendant.getPersonDetails();
            caagDefendantBuilder.withFirstName(personDetails.getFirstName())
                    .withLastName(personDetails.getLastName())
                    .withDateOfBirth(personDetails.getDateOfBirth())
                    .withAge(getAge(personDetails.getDateOfBirth()))
                    .withAddress(personDetails.getAddress())
                    .withInterpreterLanguageNeeds(personDetails.getInterpreterLanguageNeeds())
                    .withNationality(getNationalityDescription(personDetails))
                    .withArrestSummonsNumber(personDefendant.getArrestSummonsNumber())
                    .withDriverNumber(personDefendant.getDriverNumber())
                    .withGender(personDetails.getGender());

            if (nonNull(personDefendant.getBailStatus())) {
                caagDefendantBuilder.withRemandStatus(personDefendant.getBailStatus().getDescription());
            }

            if (nonNull(personDetails.getPersonMarkers())) {
                defendantMarkers.addAll(personDetails.getPersonMarkers().stream()
                        .map(Marker::getMarkerTypeDescription)
                        .collect(toList()));
            }
        }

        if (!defendantMarkers.isEmpty()) {
            caagDefendantBuilder.withDefendantMarkers(defendantMarkers);
        }

        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation())) {
            caagDefendantBuilder.withLegalEntityDefendant(legalEntityDefendant()
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
        return getResultedHearings()
                .flatMap(hearings -> hearings.getDefendants().stream())
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .flatMap(defendants -> defendants.getOffences().stream())
                .filter(offences -> offenceId.equals(offences.getId()))
                .flatMap(offences -> offences.getJudicialResults().stream())
                .collect(toList());
    }

    private Optional<Plea> getPlea(final UUID defendantId, final Offence offence) {
        final UUID offenceId = offence.getId();
        final Optional<Plea> hearingPlea = getResultedHearings()
                .flatMap(hearings -> hearings.getDefendants().stream())
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .flatMap(defendants -> defendants.getOffences().stream())
                .filter(offences -> offenceId.equals(offences.getId()) && !isEmpty(offences.getPleas()))
                .flatMap(offences -> offences.getPleas().stream())
                .filter(plea -> nonNull(plea.getPleaValue()) && nonNull(plea.getPleaDate()))
                .max(comparing(Plea::getPleaDate));
        return Optional.ofNullable(hearingPlea.orElse(offence.getPlea()));
    }

    private Optional<IndicatedPlea> getIndicatedPlea(final UUID defendantId, final Offence offence) {
        final UUID offenceId = offence.getId();
        final Optional<IndicatedPlea> hearingIndicatedPlea = getResultedHearings()
                .flatMap(hearings -> hearings.getDefendants().stream())
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .flatMap(defendants -> defendants.getOffences().stream())
                .filter(offences -> offenceId.equals(offences.getId()) && nonNull(offences.getIndicatedPlea()))
                .map(Offences::getIndicatedPlea)
                .filter(indicatedPlea -> nonNull(indicatedPlea.getIndicatedPleaValue()) && nonNull(indicatedPlea.getIndicatedPleaDate()))
                .findFirst();
        return Optional.ofNullable(hearingIndicatedPlea.orElse(offence.getIndicatedPlea()));
    }


    private Optional<Verdict> getVerdict(final UUID defendantId, final Offence offence) {
        final UUID offenceId = offence.getId();
        final Optional<Verdict> hearingVerdict = getResultedHearings()
                .flatMap(hearings -> hearings.getDefendants().stream())
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .flatMap(defendants -> defendants.getOffences().stream())
                .filter(offences -> offenceId.equals(offences.getId()) && !isEmpty(offences.getVerdicts()))
                .flatMap(offences -> offences.getVerdicts().stream())
                .filter(verdict -> nonNull(verdict.getVerdictType()) && nonNull(verdict.getVerdictDate()))
                .max(comparing(Verdict::getVerdictDate));

        return Optional.ofNullable(hearingVerdict.orElse(offence.getVerdict()));
    }

    private List<JudicialResult> getDefendantLevelJudicialResults(final UUID defendantId) {
        return getResultedHearings()
                .map(Hearings::getDefendantJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(d -> defendantId.equals(d.getMasterDefendantId()))
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private List<JudicialResult> getCaseLevelJudicialResults(final UUID defendantId) {
        return getResultedHearings()
                .map(Hearings::getDefendants)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(d -> defendantId.equals(d.getId()))
                .map(Defendants::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private Stream<Hearings> getResultedHearings() {
        return hearingsList.stream()
                .filter(hearings -> HEARING_RESULTED.equals(hearings.getHearingListingStatus()));
    }

    private Optional<Hearings> getResultedHearing() {
        return hearingsList.stream()
                .filter(hearings -> HEARING_RESULTED.equals(hearings.getHearingListingStatus())).findFirst();
    }

    private boolean isUseResultText(final String resultText){
        final String checkValue = Arrays.stream(resultText.split(" ")).limit(3).collect(Collectors.joining(" "));
        final Pattern pattern = Pattern.compile("\\w - \\w", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(checkValue);
        return matcher.find();
    }
}
