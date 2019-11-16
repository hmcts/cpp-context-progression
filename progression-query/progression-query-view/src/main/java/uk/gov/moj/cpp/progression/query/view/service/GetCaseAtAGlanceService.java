package uk.gov.moj.cpp.progression.query.view.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationResponse;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.progression.courts.CourtApplications;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.Respondents;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

@SuppressWarnings({"squid:S3655", "squid:S1188", "squid:S1135", "squid:S3776", "squid:MethodCyclomaticComplexity", "squid:S134", "squid:S4165", "pmd:NullAssignment"})
public class GetCaseAtAGlanceService {

    private static final String SPACE = " ";
    private static final String SENT_FOR_LISTING = "SENT_FOR_LISTING";

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public GetCaseAtAGlance getCaseAtAGlance(final UUID caseId) {

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseId);
        List<HearingEntity> hearingEntities = getHearingEntities(caseDefendantHearingEntities);
        hearingEntities = retrieveApplicationHearingEntities(caseId, hearingEntities);

        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        return getQueryResponse(hearingEntities, caseId, prosecutionCase, caseDefendantHearingEntities);
    }

    private List<HearingEntity> retrieveApplicationHearingEntities(final UUID caseId, final List<HearingEntity> hearingEntities) {
        final List<UUID> hearingIds = hearingEntities.stream().map(HearingEntity::getHearingId).collect(Collectors.toList());
        final List<CourtApplicationEntity> applicationEntities = courtApplicationRepository.findByLinkedCaseId(caseId);
        applicationEntities.forEach(courtApplicationEntity -> {
            final List<HearingApplicationEntity> applicationHearingEntities = hearingApplicationRepository.findByApplicationId(courtApplicationEntity.getApplicationId());
            retrieveUniqueHearings(hearingEntities, hearingIds, applicationHearingEntities);
        });
        return hearingEntities;
    }

    private void retrieveUniqueHearings(final List<HearingEntity> hearingEntities, final List<UUID> hearingIds, final List<HearingApplicationEntity> applicationHearingEntities) {
        if (CollectionUtils.isNotEmpty(applicationHearingEntities)) {
            applicationHearingEntities.forEach(hearingApplicationEntity -> {
                if (!hearingIds.contains(hearingApplicationEntity.getId().getHearingId()) && !SENT_FOR_LISTING.equals(hearingApplicationEntity.getHearing().getListingStatus().toString())) {
                    hearingEntities.add(hearingApplicationEntity.getHearing());
                    hearingIds.add(hearingApplicationEntity.getId().getHearingId());
                }
            });
        }
    }

    private List<HearingEntity> getHearingEntities(final List<CaseDefendantHearingEntity> caseDefendantHearingEntities) {
        final List<HearingEntity> hearingEntities = new ArrayList<>();
        final List<UUID> hearingIds = new ArrayList<>();
        for (final CaseDefendantHearingEntity entity : caseDefendantHearingEntities) {
            //should not show unallocated hearing
            if (!hearingIds.contains(entity.getId().getHearingId()) && !SENT_FOR_LISTING.equals(entity.getHearing().getListingStatus().toString())) {
                hearingEntities.add(entity.getHearing());
                hearingIds.add(entity.getId().getHearingId());
            }
        }
        return hearingEntities;
    }

    private GetCaseAtAGlance getQueryResponse(final List<HearingEntity> hearingEntities, final UUID caseId, final ProsecutionCase prosecutionCase, final List<CaseDefendantHearingEntity> caseDefendantHearingEntities) {

        final List<DefendantHearings> defendantHearingsList = new ArrayList<>();
        prosecutionCase.getDefendants().forEach(defendant ->
                defendantHearingsList.add(DefendantHearings.defendantHearings()
                        .withDefendantId(defendant.getId())
                        .withDefendantName(getDefendantName(defendant))
                        .withHearingIds(getHearingIdsForDefendant(caseDefendantHearingEntities, defendant))
                        .build())
        );

        return GetCaseAtAGlance.getCaseAtAGlance()
                .withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withHearings(createHearings(hearingEntities, caseId))
                .withDefendantHearings(defendantHearingsList)
                .withCourtApplications(new ArrayList<CourtApplication>())
                .build();
    }

    private static List<UUID> getHearingIdsForDefendant(final List<CaseDefendantHearingEntity> caseDefendantHearingEntities, final Defendant defendant) {
        final Set<UUID> hearingIds = caseDefendantHearingEntities.stream()
                .filter(caseDefendantHearingEntity -> caseDefendantHearingEntity.getId().getDefendantId().equals(defendant.getId()))
                .map(caseDefendantHearingEntity -> caseDefendantHearingEntity.getId().getHearingId())
                .collect(Collectors.toSet());
        return new ArrayList<>(hearingIds);
    }

    private List<Hearings> createHearings(final List<HearingEntity> hearingEntities, final UUID caseId) {
        final List<Hearings> hearingsList = new ArrayList<>();
        hearingEntities.forEach(hearingEntity -> {
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final uk.gov.justice.core.courts.Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, uk.gov.justice.core.courts.Hearing.class);

            final Hearings hearingsView = Hearings.hearings()
                    .withId(hearing.getId())
                    .withType(hearing.getType())
                    .withJurisdictionType(getJurisdictionType(hearing))
                    .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                    .withJudiciary(hearing.getJudiciary())
                    .withHearingDays(hearing.getHearingDays())
                    .withCourtCentre(hearing.getCourtCentre())
                    .withProsecutionCounsels(hearing.getProsecutionCounsels())
                    .withApplicantCounsels(hearing.getApplicantCounsels())
                    .withRespondentCounsels(hearing.getRespondentCounsels())
                    .withDefendantAttendance(hearing.getDefendantAttendance())
                    .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                    .withHearingListingStatus(getHearingListingStatus(hearingEntity))
                    .withHasResultAmended(hasResultAmended(hearing))
                    .withIsBoxHearing(hearing.getIsBoxHearing())
                    .withDefendants(createDefendants(caseId, hearing.getProsecutionCases(), hearing.getCourtApplications(), hearing))
                    .build();
            hearingsList.add(hearingsView);
        });
        return hearingsList;
    }

    private Boolean hasResultAmended(Hearing hearing) {
        if (CollectionUtils.isNotEmpty(hearing.getProsecutionCases())) {
            for (final ProsecutionCase prosecutionCase : hearing.getProsecutionCases()) {
                if (CollectionUtils.isNotEmpty(prosecutionCase.getDefendants())) {
                    for (final Defendant defendant : prosecutionCase.getDefendants()) {
                        if (CollectionUtils.isNotEmpty(defendant.getJudicialResults())) {
                            for (final JudicialResult judicialResult : defendant.getJudicialResults()) {
                                if (judicialResult.getAmendmentDate() != null) {
                                    return TRUE;
                                }
                            }
                        }
                        if (CollectionUtils.isNotEmpty(defendant.getOffences())) {
                            for (final Offence offence : defendant.getOffences()) {
                                if (CollectionUtils.isNotEmpty(offence.getJudicialResults())) {
                                    for (final JudicialResult judicialResult : offence.getJudicialResults()) {
                                        if (judicialResult.getAmendmentDate() != null) {
                                            return TRUE;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(hearing.getCourtApplications())) {
            for (final CourtApplication courtApplication : hearing.getCourtApplications()) {
                if (CollectionUtils.isNotEmpty(courtApplication.getJudicialResults())) {
                    for (final JudicialResult judicialResult : courtApplication.getJudicialResults()) {
                        if (judicialResult.getAmendmentDate() != null) {
                            return TRUE;
                        }
                    }
                }
            }
        }
        return FALSE;
    }

    private uk.gov.justice.progression.courts.JurisdictionType getJurisdictionType(final Hearing hearing) {
        if (null != hearing.getJurisdictionType()) {
            return uk.gov.justice.progression.courts.JurisdictionType.valueOf(hearing.getJurisdictionType().toString());
        }
        return null;
    }

    private HearingListingStatus getHearingListingStatus(final HearingEntity hearingEntity) {
        if (null != hearingEntity.getListingStatus()) {
            return HearingListingStatus.valueOf(hearingEntity.getListingStatus().toString());
        }
        return null;
    }

    private static List<Defendants> createDefendants(final UUID caseId, final List<ProsecutionCase> prosecutionCases, final List<CourtApplication> courtApplications, final Hearing hearing) {

        final List<Defendants> defendantsList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(prosecutionCases)) {
            final ProsecutionCase prosecutionCase = prosecutionCases.stream()
                    .filter(pc -> pc.getId().equals(caseId))
                    .findFirst().get();
            addDefendantsToDefendantsView(prosecutionCase.getDefendants(), hearing, defendantsList, courtApplications);
        }

        if (CollectionUtils.isNotEmpty(prosecutionCases) && CollectionUtils.isNotEmpty(courtApplications)) {
            courtApplications.forEach(courtApplication -> {
                final List<CourtApplicationRespondent> respondents = courtApplication.getRespondents();
                if (CollectionUtils.isNotEmpty(respondents)) {
                    addNonDefendantsToDefendantsView(courtApplications, defendantsList, respondents);
                }
            });
        }

        if (CollectionUtils.isEmpty(prosecutionCases) && CollectionUtils.isNotEmpty(courtApplications)) {
            courtApplications.forEach(courtApplication -> {
                final List<CourtApplicationRespondent> respondents = courtApplication.getRespondents();
                if (CollectionUtils.isNotEmpty(respondents)) {
                    addDefendantsAndNonDefendantsToDefendantsView(courtApplications, hearing, defendantsList, respondents);
                }
            });
        }
        return defendantsList;
    }

    private static void addNonDefendantsToDefendantsView(final List<CourtApplication> courtApplications, final List<Defendants> defendantsList, final List<CourtApplicationRespondent> respondents) {
        respondents.forEach(courtApplicationRespondent -> {
            final CourtApplicationParty courtApplicationParty = courtApplicationRespondent.getPartyDetails();
            if (Objects.nonNull(courtApplicationParty.getPersonDetails()) ||
                    Objects.nonNull(courtApplicationParty.getOrganisation()) ||
                    Objects.nonNull(courtApplicationParty.getProsecutingAuthority())) {
                defendantsList.add(getDefendantsView(courtApplications, courtApplicationParty));
            }
        });
    }

    private static void addDefendantsToDefendantsView(final List<Defendant> defendants, final Hearing hearing, final List<Defendants> defendantsList, final List<CourtApplication> courtApplications) {
        defendants.forEach(defendant -> {
            final Defendants defendantView = Defendants.defendants()
                    .withId(defendant.getId())
                    .withName(getDefendantName(defendant))
                    .withAge(getDefendantAge(defendant, hearing.getHearingDays()))
                    .withDateOfBirth(getDefendantDataOfBirth(defendant))
                    .withAddress(nonNull(defendant.getPersonDefendant()) ? defendant.getPersonDefendant().getPersonDetails().getAddress() : null)
                    .withDefenceOrganisation(getDefenceOrganisation(defendant, hearing))
                    .withOffences(getDefendantOffences(defendant))
                    .withJudicialResults(getJudicialResults(defendant.getJudicialResults()))
                    .withCourtApplications(getCourtApplicationsForDefendant(courtApplications, defendant.getId()))
                    .build();
            defendantsList.add(defendantView);
        });
    }

    private static void addDefendantsAndNonDefendantsToDefendantsView(List<CourtApplication> courtApplications, Hearing hearing, List<Defendants> defendantsList, List<CourtApplicationRespondent> respondents) {
        respondents.forEach(courtApplicationRespondent -> {
            final CourtApplicationParty courtApplicationParty = courtApplicationRespondent.getPartyDetails();
            final Defendant defendant = courtApplicationParty.getDefendant();
            if (Objects.nonNull(defendant)) {
                final Defendants defendantView = Defendants.defendants()
                        .withId(defendant.getId())
                        .withName(getDefendantName(defendant))
                        .withAge(getDefendantAge(defendant, hearing.getHearingDays()))
                        .withDateOfBirth(getDefendantDataOfBirth(defendant))
                        .withAddress(nonNull(defendant.getPersonDefendant()) ? defendant.getPersonDefendant().getPersonDetails().getAddress() : null)
                        .withDefenceOrganisation(getDefenceOrganisation(defendant, hearing))
                        .withOffences(getDefendantOffences(defendant))
                        .withJudicialResults(getJudicialResults(defendant.getJudicialResults()))
                        .withCourtApplications(getCourtApplicationsForDefendant(courtApplications, defendant.getId()))
                        .build();
                defendantsList.add(defendantView);
            }
            if (Objects.nonNull(courtApplicationParty.getPersonDetails()) ||
                    Objects.nonNull(courtApplicationParty.getOrganisation()) ||
                    Objects.nonNull(courtApplicationParty.getProsecutingAuthority())) {
                defendantsList.add(getDefendantsView(courtApplications, courtApplicationParty));
            }
        });
    }

    private static Defendants getDefendantsView(final List<CourtApplication> courtApplications, final CourtApplicationParty courtApplicationParty) {
        return Defendants.defendants()
                .withId(courtApplicationParty.getId())
                .withName(getCourtApplicationPartyName(courtApplicationParty))
                .withCourtApplications(getCourtApplicationsForNonDefendant(courtApplications, courtApplicationParty.getId()))
                .withJudicialResults(getJudicialResults(null))
                .build();
    }

    private static List<CourtApplications> getCourtApplicationsForDefendant(final List<CourtApplication> courtApplications, final UUID defendantId) {
        List<CourtApplications> courtApplicationsViewList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(courtApplications)) {
            courtApplicationsViewList = new ArrayList<>();
            for (final CourtApplication courtApplication : courtApplications) {
                if (CollectionUtils.isNotEmpty(courtApplication.getRespondents())) {
                    for (final CourtApplicationRespondent courtApplicationRespondent : courtApplication.getRespondents()) {
                        final Defendant defendant = courtApplicationRespondent.getPartyDetails().getDefendant();
                        if (nonNull(defendant) && defendant.getId().equals(defendantId)) {
                            courtApplicationsViewList.add(CourtApplications.courtApplications()
                                    .withApplicationId(courtApplication.getId())
                                    .withApplicant(getCourtApplicationPartyName(courtApplication.getApplicant()))
                                    .withRespondents(getRespondents(courtApplication.getRespondents()))
                                    .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                                    .withApplicationType(courtApplication.getType().getApplicationType())
                                    .withIsAppeal(courtApplication.getType().getIsAppealApplication())
                                    .withJudicialResults(courtApplication.getJudicialResults())
                                    .withOutcome(nonNull(courtApplication.getApplicationOutcome()) ? courtApplication.getApplicationOutcome().getApplicationOutcomeType().getDescription() : null)
                                    .withOutcomeDate(nonNull(courtApplication.getApplicationOutcome()) ? courtApplication.getApplicationOutcome().getApplicationOutcomeDate() : null)
                                    .build());
                        }
                    }
                }
            }
        }
        return courtApplicationsViewList;
    }

    private static List<CourtApplications> getCourtApplicationsForNonDefendant(final List<CourtApplication> courtApplications, final UUID partyDetailsId) {
        final List<CourtApplications> courtApplicationsViewList = new ArrayList<>();
        courtApplications.forEach(courtApplication -> {
            if (CollectionUtils.isNotEmpty(courtApplication.getRespondents())) {
                courtApplication.getRespondents().forEach(courtApplicationRespondent -> {
                    if (partyDetailsId.equals(courtApplicationRespondent.getPartyDetails().getId())) {
                        courtApplicationsViewList.add(CourtApplications.courtApplications()
                                .withApplicationId(courtApplication.getId())
                                .withApplicant(getCourtApplicationPartyName(courtApplication.getApplicant()))
                                .withRespondents(getRespondents(courtApplication.getRespondents()))
                                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                                .withApplicationType(courtApplication.getType().getApplicationType())
                                .withIsAppeal(courtApplication.getType().getIsAppealApplication())
                                .withJudicialResults(getJudicialResults(courtApplication.getJudicialResults()))
                                .withOutcome(nonNull(courtApplication.getApplicationOutcome()) ? courtApplication.getApplicationOutcome().getApplicationOutcomeType().getDescription() : null)
                                .withOutcomeDate(nonNull(courtApplication.getApplicationOutcome()) ? courtApplication.getApplicationOutcome().getApplicationOutcomeDate() : null)
                                .build());
                    }
                });
            }
        });
        return courtApplicationsViewList;
    }

    private static List<Respondents> getRespondents(final List<CourtApplicationRespondent> courtApplicationRespondents) {
        final List<Respondents> respondents = new ArrayList<>();
        courtApplicationRespondents.forEach(courtApplicationRespondent ->
                respondents.add(Respondents.respondents()
                        .withName(getCourtApplicationPartyName(courtApplicationRespondent.getPartyDetails()))
                        .withApplicationResponse(getApplicationResponse(courtApplicationRespondent.getApplicationResponse()))
                        .withResponseDate(getResponseDate(courtApplicationRespondent.getApplicationResponse()))
                        .build()));
        return respondents;
    }

    private static LocalDate getResponseDate(final CourtApplicationResponse applicationResponse) {
        LocalDate responseDate = null;
        if (Objects.nonNull(applicationResponse)) {
            responseDate = applicationResponse.getApplicationResponseDate();
        }
        return responseDate;
    }

    private static String getApplicationResponse(final CourtApplicationResponse applicationResponse) {
        String response = null;
        if (Objects.nonNull(applicationResponse) && Objects.nonNull(applicationResponse.getApplicationResponseType())) {
            response = applicationResponse.getApplicationResponseType().getDescription();
        }
        return response;
    }

    private static String getCourtApplicationPartyName(final CourtApplicationParty courtApplicationParty) {
        final Defendant defendant = courtApplicationParty.getDefendant();
        if (nonNull(defendant) && nonNull(defendant.getPersonDefendant())) {
            return getPersonName(defendant.getPersonDefendant().getPersonDetails());
        }
        if (nonNull(courtApplicationParty.getPersonDetails())) {
            return getPersonName(courtApplicationParty.getPersonDetails());
        }
        if (nonNull(courtApplicationParty.getOrganisation())) {
            return courtApplicationParty.getOrganisation().getName();
        }
        if (nonNull(courtApplicationParty.getProsecutingAuthority())) {
            return courtApplicationParty.getProsecutingAuthority().getProsecutionAuthorityCode();
        }
        return null;
    }

    private static List<Offences> getDefendantOffences(final Defendant defendant) {
        final List<Offences> offencesList = new ArrayList<>();
        defendant.getOffences().forEach(offence -> {
            final Offences offences = Offences.offences()
                    .withId(offence.getId())
                    .withOffenceDefinitionId(offence.getOffenceDefinitionId())
                    .withOffenceCode(offence.getOffenceCode())
                    .withOffenceTitle(offence.getOffenceTitle())
                    .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                    .withOffenceLegislation(offence.getOffenceLegislation())
                    .withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh())
                    .withWording(offence.getWording())
                    .withWordingWelsh(offence.getWordingWelsh())
                    .withStartDate(offence.getStartDate())
                    .withEndDate(offence.getEndDate())
                    .withCount(offence.getCount())
                    .withConvictionDate(offence.getConvictionDate())
                    .withNotifiedPlea(offence.getNotifiedPlea())
                    .withIndicatedPlea(offence.getIndicatedPlea())
                    .withAllocationDecision(offence.getAllocationDecision())
                    .withPleas(getOffencePleas(offence.getPlea()))
                    .withVerdicts(getOffenceVerdicts(offence.getVerdict()))
                    .withJudicialResults(getJudicialResults(offence.getJudicialResults()))
                    .withAcquittalDate(offence.getAquittalDate())
                    .build();
            offencesList.add(offences);
        });
        return offencesList;
    }

    private static List<Verdict> getOffenceVerdicts(final Verdict verdict) {
        final List<Verdict> verdicts = new ArrayList<>();
        if (nonNull(verdict)) {
            verdicts.add(verdict);
        }
        return verdicts;
    }

    private static List<Plea> getOffencePleas(final Plea plea) {
        final List<Plea> pleas = new ArrayList<>();
        if (nonNull(plea)) {
            pleas.add(plea);
        }
        return pleas;
    }

    private static List<JudicialResult> getJudicialResults(final List<JudicialResult> judicialResults) {
        final List<JudicialResult> results = new ArrayList<>();
        if (nonNull(judicialResults)) {
            results.addAll(judicialResults);
        }
        return results;
    }

    private static String getDefendantAge(final uk.gov.justice.core.courts.Defendant defendant, final List<HearingDay> hearingDays) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())) {
            final String dateOfBirthText = defendant.getPersonDefendant().getPersonDetails().getDateOfBirth().toString();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());
            final LocalDate dateOfBirth = LocalDate.parse(dateOfBirthText, formatter);
            final ZonedDateTime earliestHearingDay = getEarliestHearingDate(hearingDays);
            if(Objects.nonNull(earliestHearingDay)) {
                final Period period = Period.between(dateOfBirth, earliestHearingDay.toLocalDate());
                return Integer.toString(period.getYears());
            }
        }
        return EMPTY;
    }

    private static ZonedDateTime getEarliestHearingDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream().sorted(comparing(HearingDay::getSittingDay)).findFirst().get().getSittingDay();
    }

    private static LocalDate getDefendantDataOfBirth(final uk.gov.justice.core.courts.Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getDateOfBirth();
        }
        return null;
    }

    private static DefenceOrganisation getDefenceOrganisation(final uk.gov.justice.core.courts.Defendant defendant, final uk.gov.justice.core.courts.Hearing hearing) {
        return DefenceOrganisation.defenceOrganisation()
                .withDefendantId(defendant.getId())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withDefenceCounsels(getDefenceCounselsForDefendant(defendant, hearing.getDefenceCounsels()))
                .build();
    }

    private static List<DefenceCounsel> getDefenceCounselsForDefendant(final uk.gov.justice.core.courts.Defendant defendant, final List<DefenceCounsel> defenceCounsels) {
        if (null != defenceCounsels) {
            return defenceCounsels.stream()
                    .filter(defenceCounsel -> defenceCounsel.getDefendants().contains(defendant.getId()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private static String getDefendantName(final uk.gov.justice.core.courts.Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            return getPersonName(personDefendant.getPersonDetails());
        }
        if (nonNull(defendant.getLegalEntityDefendant())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getName();
        }
        return EMPTY;
    }

    private static String getPersonName(final Person person) {
        final StringBuilder nameBuilder = new StringBuilder();
        if (nonNull(person)) {
            if (nonNull(person.getFirstName())) {
                nameBuilder.append(person.getFirstName());
            }
            if (nonNull(person.getMiddleName())) {
                if (!nameBuilder.toString().isEmpty()) {
                    nameBuilder.append(SPACE);
                }
                nameBuilder.append(person.getMiddleName());
            }
            if (nonNull(person.getLastName())) {
                if (!nameBuilder.toString().isEmpty()) {
                    nameBuilder.append(SPACE);
                }
                nameBuilder.append(person.getLastName());
            }
        }
        return nameBuilder.toString();
    }
}
