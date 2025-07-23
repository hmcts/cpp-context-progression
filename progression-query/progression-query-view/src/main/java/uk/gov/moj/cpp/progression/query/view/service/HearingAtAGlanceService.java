package uk.gov.moj.cpp.progression.query.view.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
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
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.Respondents;
import uk.gov.justice.progression.query.TrialDefendants;
import uk.gov.justice.progression.query.TrialHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1188", "squid:S1135", "squid:S3776", "squid:MethodCyclomaticComplexity", "squid:S134", "squid:S4165", "pmd:NullAssignment", "squid:CommentedOutCodeLine"})
public class HearingAtAGlanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAtAGlanceService.class.getCanonicalName());

    private static final String SPACE = " ";
    private static final String HEARING_STATUS_SENT_FOR_LISTING = "SENT_FOR_LISTING";

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public List<Hearings> getCaseHearings(final UUID caseId) {
        LOGGER.info("Get case hearings for case {}", caseId);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseId);
        final List<HearingEntity> hearingEntities = getHearingEntities(caseDefendantHearingEntities);
        return createHearings(hearingEntities, caseId);
    }

    public List<Hearings> getCaseDefendantHearings(final UUID caseId, final UUID defendantId) {
        LOGGER.info("Get case hearings for case={} and defendant={}", caseId, defendantId);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseIdAndDefendantId(caseId, defendantId);
        return toHearings(getHearingEntities(caseDefendantHearingEntities));
    }

    public GetHearingsAtAGlance getHearingAtAGlance(final UUID caseId) {
        LOGGER.info("Get hearings for case {}", caseId);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseId);
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        JurisdictionType latestHearingJurisdictionType = null;
        final List<HearingEntity> hearingEntities = getHearingEntities(caseDefendantHearingEntities);
        addApplicationHearingEntities(caseId, hearingEntities);

        if (!hearingEntities.isEmpty()) {
            latestHearingJurisdictionType = getLatestHearingJurisdictionType(caseId, hearingEntities);
            LOGGER.info("Retrieved the latest hearing jurisdiction type {}", latestHearingJurisdictionType);
        }

        return getQueryResponse(hearingEntities, caseId, prosecutionCase, caseDefendantHearingEntities, latestHearingJurisdictionType);
    }

    public List<TrialHearing> getTrialHearings(final UUID prosecutionCaseId) {
        LOGGER.info("Get trial hearings for prosecution case id {}", prosecutionCaseId);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(prosecutionCaseId);
        final List<HearingEntity> hearingEntities = getHearingEntities(caseDefendantHearingEntities);
        return createTrialHearings(hearingEntities, prosecutionCaseId);
    }

    private List<HearingEntity> getHearingEntities(final List<CaseDefendantHearingEntity> caseDefendantHearingEntities) {
        return caseDefendantHearingEntities.stream().map(CaseDefendantHearingEntity::getHearing).distinct()
                .sorted(Comparator.comparing(HearingEntity::getSharedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(toList());
    }

    private JurisdictionType getLatestHearingJurisdictionType(final UUID caseId, final List<HearingEntity> hearingEntities) {
        final List<Hearings> hearingsList = createHearings(hearingEntities, caseId);
        final List<Hearings> hearingsWithHearingDays = hearingsList
                .stream()
                .filter(h -> nonNull(h.getHearingDays()))
                .collect(toList());

        hearingsWithHearingDays
                .sort(comparing(hearings -> hearings.getHearingDays()
                        .stream()
                        .map(HearingDay::getSittingDay)
                        .sorted()
                        .findFirst()
                        .<IllegalArgumentException>orElseThrow(IllegalArgumentException::new)
                ));

        JurisdictionType jurisdictionType = null;

        if (!hearingsWithHearingDays.isEmpty()) {
            jurisdictionType = hearingsWithHearingDays.get(0).getJurisdictionType();
        }

        if (isNull(jurisdictionType)) {
            jurisdictionType = hearingsList.get(0).getJurisdictionType();
        }

        return JurisdictionType.valueOf(jurisdictionType.toString());
    }

    private void addApplicationHearingEntities(final UUID caseId, final List<HearingEntity> hearingEntities) {
        LOGGER.info("Retrieve application hearings for case {}", caseId);
        final List<UUID> hearingIds = hearingEntities.stream().map(HearingEntity::getHearingId).collect(toList());
        final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(caseId);
        for (final CourtApplicationCaseEntity courtApplicationCaseEntity : courtApplicationCaseEntities) {
            final List<HearingApplicationEntity> applicationHearingEntities = hearingApplicationRepository.findByApplicationId(courtApplicationCaseEntity.getId().getApplicationId());

            if (isNotEmpty(applicationHearingEntities)) {
                for (final HearingApplicationEntity hearingApplicationEntity : applicationHearingEntities) {
                    if (!hearingIds.contains(hearingApplicationEntity.getId().getHearingId())) {
                        hearingEntities.add(hearingApplicationEntity.getHearing());
                        hearingIds.add(hearingApplicationEntity.getId().getHearingId());
                        LOGGER.info("Added court application hearingId {}", hearingApplicationEntity.getId().getHearingId());
                    }
                }
            }
        }
    }

    private GetHearingsAtAGlance getQueryResponse(final List<HearingEntity> hearingEntities, final UUID caseId,
                                                  final ProsecutionCase prosecutionCase,
                                                  final List<CaseDefendantHearingEntity> caseDefendantHearingEntities,
                                                  final JurisdictionType latestHearingJurisdictionType) {

        final List<DefendantHearings> defendantHearingsList = new ArrayList<>();
        prosecutionCase.getDefendants().forEach(defendant ->
                defendantHearingsList.add(DefendantHearings.defendantHearings()
                        .withDefendantId(defendant.getId())
                        .withDefendantName(getDefendantName(defendant.getPersonDefendant(), defendant.getLegalEntityDefendant()))
                        .withHearingIds(getHearingIdsForDefendant(caseDefendantHearingEntities, defendant))
                        .build())
        );

        return GetHearingsAtAGlance.getHearingsAtAGlance()
                .withId(caseId)
                .withLatestHearingJurisdictionType(latestHearingJurisdictionType)
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withHearings(createHearings(hearingEntities, caseId))
                .withDefendantHearings(defendantHearingsList)
                .withCourtApplications(new ArrayList<>())
                .build();
    }

    private static List<UUID> getHearingIdsForDefendant(final List<CaseDefendantHearingEntity> caseDefendantHearingEntities, final Defendant defendant) {
        final Set<UUID> hearingIds = caseDefendantHearingEntities.stream()
                .filter(caseDefendantHearingEntity -> caseDefendantHearingEntity.getId().getDefendantId().equals(defendant.getId()))
                .map(caseDefendantHearingEntity -> caseDefendantHearingEntity.getId().getHearingId())
                .collect(toSet());
        return new ArrayList<>(hearingIds);
    }

    private List<Hearings> createHearings(final List<HearingEntity> hearingEntities, final UUID caseId) {
        LOGGER.info("Create hearings for case {}", caseId);
        final List<Hearings> hearingsList = new ArrayList<>();
        hearingEntities.forEach(hearingEntity -> {
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            LOGGER.info("Create hearing for hearingId {}", hearing.getId());
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
                    .withDefendantJudicialResults(hearing.getDefendantJudicialResults())
                    .withDefendants(createDefendants(caseId, hearing.getProsecutionCases(), hearing.getCourtApplications(), hearing))
                    .withYouthCourt(hearing.getYouthCourt())
                    .withYouthCourtDefendantIds(hearing.getYouthCourtDefendantIds())
                    .withIsApplicationHearing(CollectionUtils.isNotEmpty(hearing.getCourtApplications()))
                    .withCompanyRepresentatives(hearing.getCompanyRepresentatives())
                    .build();
            hearingsList.add(hearingsView);
        });
        return hearingsList;
    }

    private List<Hearings> toHearings(List<HearingEntity> hearingEntities) {
        final List<Hearings> hearingsList = new ArrayList<>();
        hearingEntities.forEach(hearingEntity -> {
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final uk.gov.justice.core.courts.Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, uk.gov.justice.core.courts.Hearing.class);

            final Hearings hearingsView = Hearings.hearings()
                    .withId(hearing.getId())
                    .withHearingDays(hearing.getHearingDays())
                    .withCourtCentre(hearing.getCourtCentre())
                    .withHearingListingStatus(getHearingListingStatus(hearingEntity))
                    .build();
            hearingsList.add(hearingsView);
        });
        return hearingsList;
    }

    private List<TrialHearing> createTrialHearings(final List<HearingEntity> hearingEntities, final UUID prosecutionCaseId) {
        LOGGER.info("Create trial hearings for prosecution case {}", prosecutionCaseId);
        final List<TrialHearing> trialHearings = new ArrayList<>();
        hearingEntities.forEach(hearingEntity -> {
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);

            final TrialHearing trialHearing = TrialHearing.trialHearing()
                    .withId(hearing.getId())
                    .withType(hearing.getType())
                    .withHearingDay(getEarliestHearingDate(hearing.getHearingDays()))
                    .withJurisdictionType(getJurisdictionType(hearing))
                    .withCourtCentre(hearing.getCourtCentre())
                    .withTrialDefendants(createTrialHearingDefendants(prosecutionCaseId, hearing.getProsecutionCases()))
                    .build();
            trialHearings.add(trialHearing);
        });
        return trialHearings;
    }

    private Boolean hasResultAmended(final Hearing hearing) {
        if (isNotEmpty(hearing.getProsecutionCases())) {
            for (final ProsecutionCase prosecutionCase : hearing.getProsecutionCases()) {
                if (isNotEmpty(prosecutionCase.getDefendants())) {
                    for (final Defendant defendant : prosecutionCase.getDefendants()) {
                        if (isNotEmpty(defendant.getDefendantCaseJudicialResults())) {
                            for (final JudicialResult judicialResult : defendant.getDefendantCaseJudicialResults()) {
                                if (judicialResult.getAmendmentDate() != null) {
                                    return TRUE;
                                }
                            }
                        }
                        if (isNotEmpty(defendant.getOffences())) {
                            for (final Offence offence : defendant.getOffences()) {
                                if (isNotEmpty(offence.getJudicialResults())) {
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
        if (isNotEmpty(hearing.getCourtApplications())) {
            for (final CourtApplication courtApplication : hearing.getCourtApplications()) {
                if (isNotEmpty(courtApplication.getJudicialResults())) {
                    for (final JudicialResult judicialResult : courtApplication.getJudicialResults()) {
                        if (judicialResult.getAmendmentDate() != null) {
                            return TRUE;
                        }
                    }
                }
            }
        }
        if (isNotEmpty(hearing.getDefendantJudicialResults())) {
            for (final DefendantJudicialResult defendantJudicialResult : hearing.getDefendantJudicialResults()) {
                if (nonNull(defendantJudicialResult.getJudicialResult()) && defendantJudicialResult.getJudicialResult().getAmendmentDate() != null) {
                    return TRUE;
                }
            }
        }
        return FALSE;
    }

    private JurisdictionType getJurisdictionType(final Hearing hearing) {
        if (null != hearing.getJurisdictionType()) {
            return JurisdictionType.valueOf(hearing.getJurisdictionType().toString());
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
        LOGGER.info("Create defendants for case {}", caseId);
        final List<Defendants> defendantsList = new ArrayList<>();
        if (isNotEmpty(prosecutionCases)) {
            final ProsecutionCase prosecutionCase = prosecutionCases.stream()
                    .filter(pc -> nonNull(pc) && caseId.equals(pc.getId()))
                    .findFirst().orElse(null);
            if (nonNull(prosecutionCase)) {
                addDefendantsToDefendantsView(prosecutionCase.getDefendants(), hearing, defendantsList, courtApplications);
            }
        }

        if (isNotEmpty(prosecutionCases) && isNotEmpty(courtApplications)) {
            courtApplications.forEach(courtApplication -> {
                final List<CourtApplicationParty> respondents = courtApplication.getRespondents();
                if (isNotEmpty(respondents)) {
                    addNonDefendantsToDefendantsView(courtApplications, defendantsList, respondents);
                }
            });
        }

        if (CollectionUtils.isEmpty(prosecutionCases) && isNotEmpty(courtApplications)) {
            courtApplications.forEach(courtApplication -> {
                final List<CourtApplicationParty> respondents = courtApplication.getRespondents();
                if (isNotEmpty(respondents)) {
                    addDefendantsAndNonDefendantsToDefendantsView(courtApplications, hearing, defendantsList, respondents);
                }
            });
        }
        return defendantsList;
    }

    private List<TrialDefendants> createTrialHearingDefendants(final UUID prosecutionCaseId, final List<ProsecutionCase> prosecutionCases) {
        LOGGER.info("Create defendants for prosecution case {}", prosecutionCaseId);
        final List<TrialDefendants> trialDefendants = new ArrayList<>();
        if (isNotEmpty(prosecutionCases)) {
            final ProsecutionCase prosecutionCase = prosecutionCases.stream()
                    .filter(pc -> nonNull(pc) && prosecutionCaseId.equals(pc.getId()))
                    .findFirst()
                    .orElse(null);
            if (nonNull(prosecutionCase)) {
                addDefendantsToTrialHearing(prosecutionCase.getDefendants(), trialDefendants);
            }
        }
        return trialDefendants;
    }

    private static void addNonDefendantsToDefendantsView(final List<CourtApplication> courtApplications, final List<Defendants> defendantsList, final List<CourtApplicationParty> courtApplicationParties) {
        courtApplicationParties.forEach(courtApplicationParty -> {
            if (nonNull(courtApplicationParty.getPersonDetails()) ||
                    nonNull(courtApplicationParty.getOrganisation()) ||
                    nonNull(courtApplicationParty.getProsecutingAuthority())) {
                defendantsList.add(getDefendantsView(courtApplications, courtApplicationParty));
            }
        });
    }

    private static void addDefendantsToDefendantsView(final List<Defendant> defendants, final Hearing hearing, final List<Defendants> defendantsList, final List<CourtApplication> courtApplications) {
        LOGGER.info("Add defendants to defendants view");
        defendants.forEach(defendant -> {
            final Defendants defendantView = Defendants.defendants()
                    .withId(defendant.getId())
                    .withName(getDefendantName(defendant.getPersonDefendant(), defendant.getLegalEntityDefendant()))
                    .withAge(getDefendantAge(defendant.getPersonDefendant(), hearing.getHearingDays()))
                    .withDateOfBirth(getDefendantDataOfBirth(defendant.getPersonDefendant()))
                    .withAddress(extractAddress(defendant))
                    .withDefenceOrganisation(getDefenceOrganisation(defendant, hearing))
                    .withOffences(getDefendantOffences(defendant))
                    .withJudicialResults(getJudicialResults(defendant.getDefendantCaseJudicialResults()))
                    .withCourtApplications(getCourtApplicationsForDefendant(courtApplications, defendant.getId()))
                    .withLegalAidStatus(defendant.getLegalAidStatus())
                    .withProceedingsConcluded(defendant.getProceedingsConcluded())
                    .build();
            defendantsList.add(defendantView);
        });
    }

    private static void addDefendantsToTrialHearing(final List<Defendant> defendants, final List<TrialDefendants> defendantsList) {
        defendants.forEach(defendant -> {
            final TrialDefendants trialDefendants = TrialDefendants.trialDefendants()
                    .withId(defendant.getId())
                    .withFullName(getDefendantName(defendant.getPersonDefendant(), defendant.getLegalEntityDefendant()))
                    .withDateOfBirth(getDefendantDataOfBirth(defendant.getPersonDefendant()))
                    .build();
            defendantsList.add(trialDefendants);
        });
    }

    private static Address extractAddress(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            return defendant.getPersonDefendant().getPersonDetails().getAddress();
        } else if (nonNull(defendant.getLegalEntityDefendant())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getAddress();
        } else {
            return null;
        }
    }

    private static void addDefendantsAndNonDefendantsToDefendantsView(final List<CourtApplication> courtApplications, final Hearing hearing, final List<Defendants> defendantsList, final List<CourtApplicationParty> courtApplicationParties) {
        courtApplicationParties.forEach(courtApplicationParty -> {
            final MasterDefendant masterDefendant = courtApplicationParty.getMasterDefendant();
            if (nonNull(masterDefendant)) {
                final Defendants defendantView = Defendants.defendants()
                        .withId(masterDefendant.getMasterDefendantId())
                        .withName(getDefendantName(masterDefendant.getPersonDefendant(), masterDefendant.getLegalEntityDefendant()))
                        .withAge(getDefendantAge(masterDefendant.getPersonDefendant(), hearing.getHearingDays()))
                        .withDateOfBirth(getDefendantDataOfBirth(masterDefendant.getPersonDefendant()))
                        .withAddress(nonNull(masterDefendant.getPersonDefendant()) ? masterDefendant.getPersonDefendant().getPersonDetails().getAddress() : null)
                        .withJudicialResults(getJudicialResults(null))
                        .withCourtApplications(getCourtApplicationsForDefendant(courtApplications, masterDefendant.getMasterDefendantId()))
                        .build();
                defendantsList.add(defendantView);
            }

            if (nonNull(courtApplicationParty.getPersonDetails()) ||
                    nonNull(courtApplicationParty.getOrganisation()) ||
                    nonNull(courtApplicationParty.getProsecutingAuthority())) {
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
        if (isNotEmpty(courtApplications)) {
            courtApplicationsViewList = new ArrayList<>();
            for (final CourtApplication courtApplication : courtApplications) {
                if (isNotEmpty(courtApplication.getRespondents())) {
                    for (final CourtApplicationParty courtApplicationParty : courtApplication.getRespondents()) {
                        final MasterDefendant masterDefendant = courtApplicationParty.getMasterDefendant();
                        if (nonNull(masterDefendant) && masterDefendant.getMasterDefendantId().equals(defendantId)) {
                            courtApplicationsViewList.add(CourtApplications.courtApplications()
                                    .withApplicationId(courtApplication.getId())
                                    .withApplicant(getCourtApplicationPartyName(courtApplication.getApplicant()))
                                    .withRespondents(getRespondents(courtApplication.getRespondents()))
                                    .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                                    .withApplicationType(courtApplication.getType().getType())
                                    .withIsAppeal(courtApplication.getType().getAppealFlag())
                                    .withJudicialResults(courtApplication.getJudicialResults())
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
            if (isNotEmpty(courtApplication.getRespondents())) {
                courtApplication.getRespondents().forEach(courtApplicationRespondent -> {
                    if (partyDetailsId.equals(courtApplicationRespondent.getId())) {
                        courtApplicationsViewList.add(CourtApplications.courtApplications()
                                .withApplicationId(courtApplication.getId())
                                .withApplicant(getCourtApplicationPartyName(courtApplication.getApplicant()))
                                .withRespondents(getRespondents(courtApplication.getRespondents()))
                                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                                .withApplicationType(courtApplication.getType().getType())
                                .withIsAppeal(courtApplication.getType().getAppealFlag())
                                .withJudicialResults(getJudicialResults(courtApplication.getJudicialResults()))
                                .build());
                    }
                });
            }
        });
        return courtApplicationsViewList;
    }

    private static List<Respondents> getRespondents(final List<CourtApplicationParty> courtApplicationParties) {
        final List<Respondents> respondents = new ArrayList<>();
        courtApplicationParties.forEach(courtApplicationParty ->
                respondents.add(Respondents.respondents()
                        .withName(getCourtApplicationPartyName(courtApplicationParty))
                        .build()));
        return respondents;
    }

    private static String getCourtApplicationPartyName(final CourtApplicationParty courtApplicationParty) {
        final MasterDefendant masterDefendant = courtApplicationParty.getMasterDefendant();
        if (nonNull(masterDefendant) && nonNull(masterDefendant.getPersonDefendant())) {
            return getPersonName(masterDefendant.getPersonDefendant().getPersonDetails());
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
                    .withOrderIndex(offence.getOrderIndex())
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
                    .withConvictingCourt(offence.getConvictingCourt())
                    .withNotifiedPlea(offence.getNotifiedPlea())
                    .withIndicatedPlea(offence.getIndicatedPlea())
                    .withAllocationDecision(offence.getAllocationDecision())
                    .withPleas(getOffencePleas(offence.getPlea()))
                    .withVerdicts(getOffenceVerdicts(offence.getVerdict()))
                    .withJudicialResults(getJudicialResults(offence.getJudicialResults()))
                    .withAcquittalDate(offence.getAquittalDate())
                    .withLaaApplnReference(offence.getLaaApplnReference())
                    .withProceedingsConcluded(offence.getProceedingsConcluded())
                    .withIndictmentParticular(offence.getIndictmentParticular())
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

    private static String getDefendantAge(final PersonDefendant personDefendant, final List<HearingDay> hearingDays) {
        LOGGER.debug("Calculate defendant age for defendant {} ", personDefendant);
        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails().getDateOfBirth())
                && isNotEmpty(hearingDays)) {
            LOGGER.info("Count of hearing days provided {}", hearingDays.size());
            final String dateOfBirthText = personDefendant.getPersonDetails().getDateOfBirth().toString();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());
            final LocalDate dateOfBirth = LocalDate.parse(dateOfBirthText, formatter);
            final ZonedDateTime earliestHearingDay = getEarliestHearingDate(hearingDays);
            if (nonNull(earliestHearingDay)) {
                final Period period = Period.between(dateOfBirth, earliestHearingDay.toLocalDate());
                LOGGER.info("Defendant age is calculated {}", period.getYears());
                return Integer.toString(period.getYears());
            }
        }
        LOGGER.info("Defendant age is not calculated");
        return EMPTY;
    }

    private static ZonedDateTime getEarliestHearingDate(final List<HearingDay> hearingDays) {
        LOGGER.info("Get earliest hearing date");
        return hearingDays.stream().sorted(comparing(HearingDay::getSittingDay)).findFirst().get().getSittingDay();
    }

    private static LocalDate getDefendantDataOfBirth(final PersonDefendant personDefendant) {
        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails())) {
            return personDefendant.getPersonDetails().getDateOfBirth();
        }
        return null;
    }

    private static DefenceOrganisation getDefenceOrganisation(final Defendant defendant, final Hearing hearing) {
        return DefenceOrganisation.defenceOrganisation()
                .withDefendantId(defendant.getId())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withDefenceCounsels(getDefenceCounselsForDefendant(defendant, hearing.getDefenceCounsels()))
                .build();
    }

    private static List<DefenceCounsel> getDefenceCounselsForDefendant(final Defendant defendant, final List<DefenceCounsel> defenceCounsels) {
        if (null != defenceCounsels) {
            return defenceCounsels.stream()
                    .filter(defenceCounsel -> defenceCounsel.getDefendants().contains(defendant.getId()))
                    .collect(toList());
        }
        return new ArrayList<>();
    }

    public static String getDefendantName(final PersonDefendant personDefendant, final LegalEntityDefendant legalEntityDefendant) {
        if (nonNull(personDefendant)) {
            return getPersonName(personDefendant.getPersonDetails());
        }
        if (nonNull(legalEntityDefendant)) {
            return legalEntityDefendant.getOrganisation().getName();
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

    public List<HearingEntity> getCaseHearingEntities(final UUID caseId) {
        LOGGER.info("Get case Hearing entities for case {}", caseId);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseId);
        return getHearingEntities(caseDefendantHearingEntities);
    }
}
