package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655", "squid:S1188"})
public class GetCaseAtAGlanceService {

    private static final String OFFENCE = "OFFENCE";
    private static final String SPACE = " ";

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public GetCaseAtAGlance getCaseAtAGlance(final UUID caseId) {

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseId);
        final List<HearingEntity> hearingEntities = getHearingEntities(caseDefendantHearingEntities);

        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        return getQueryResponse(hearingEntities, caseId, prosecutionCase, caseDefendantHearingEntities);
    }

    private List<HearingEntity> getHearingEntities(final List<CaseDefendantHearingEntity> caseDefendantHearingEntities) {
        final List<HearingEntity> hearingEntities = new ArrayList<>();
        final List<UUID> hearingIds = new ArrayList<>();
        for (final CaseDefendantHearingEntity entity : caseDefendantHearingEntities) {
            //should not show unallocated hearing
            if(!hearingIds.contains(entity.getId().getHearingId()) && !"SENT_FOR_LISTING".equals(entity.getHearing().getListingStatus().toString())) {
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

            final Set<HearingResultLineEntity> hearingResultLineEntities = hearingEntity.getResultLines();
            final Set<SharedResultLine> sharedResultLines = new HashSet<>();
            hearingResultLineEntities.stream().forEach(hearingResultLineEntity -> {
                final JsonObject hearingResultLineJson = stringToJsonObjectConverter.convert(hearingResultLineEntity.getPayload());
                final SharedResultLine sharedResultLine = jsonObjectToObjectConverter.convert(hearingResultLineJson, uk.gov.justice.core.courts.SharedResultLine.class);
                sharedResultLines.add(sharedResultLine);
            });

            final Hearings hearingsView = Hearings.hearings()
                    .withId(hearing.getId())
                    .withType(hearing.getType().getDescription())
                    .withJurisdictionType(getJurisdictionType(hearing))
                    .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                    .withJudiciary(hearing.getJudiciary())
                    .withHearingDays(hearing.getHearingDays())
                    .withCourtCentre(hearing.getCourtCentre())
                    .withProsecutionCounsels(hearing.getProsecutionCounsels())
                    .withDefendantAttendance(hearing.getDefendantAttendance())
                    .withHearingListingStatus(getHearingListingStatus(hearingEntity))
                    .withDefendants(createDefendants(caseId, hearing.getProsecutionCases(), hearing, sharedResultLines))
                    .build();
            hearingsList.add(hearingsView);
        });
        return hearingsList;
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

    private static List<Defendants> createDefendants(final UUID caseId, final List<ProsecutionCase> prosecutionCases, final uk.gov.justice.core.courts.Hearing hearing, final Set<SharedResultLine> sharedResultLines) {

        final List<Defendants> defendantsList = new ArrayList<>();
        final ProsecutionCase prosecutionCase = prosecutionCases.stream()
                .filter(pc -> pc.getId().equals(caseId))
                .findFirst().get();

        prosecutionCase.getDefendants().stream().forEach(defendant -> {
            final Defendants defendantView = Defendants.defendants()
                    .withId(defendant.getId())
                    .withName(getDefendantName(defendant))
                    .withAge(getDefendantAge(defendant))
                    .withDateOfBirth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())
                    .withAddress(defendant.getPersonDefendant().getPersonDetails().getAddress())
                    .withDefenceOrganisation(getDefenceOrganisation(defendant, hearing))
                    .withOffences(getDefendantOffences(defendant, sharedResultLines))
                    .withResults(getCaseLevelSharedResults(defendant, prosecutionCase, sharedResultLines))
                    .build();
            defendantsList.add(defendantView);
        });
        return defendantsList;
    }

    private static List<Offences> getDefendantOffences(final uk.gov.justice.core.courts.Defendant defendant, final Set<SharedResultLine> sharedResultLines) {
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
                            .withPleas(getOffencePleas(offence.getPlea()))
                            .withVerdicts(getOffenceVerdicts(offence.getVerdict()))
                            .withResults(getOffenceLevelSharedResults(offence, sharedResultLines))
                            .withIsAcquitted(offence.getIsAcquitted())
                            .build();
                    offencesList.add(offences);
                });
        return offencesList;
    }

    private static List<Verdict> getOffenceVerdicts(final Verdict verdict) {
        final List<Verdict> verdicts = new ArrayList<>();
        if(nonNull(verdict)){
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

    private static List<SharedResultLine> getOffenceLevelSharedResults(final Offence offence, final Set<SharedResultLine> sharedResultLines) {
        return sharedResultLines.stream()
                .filter(sharedResultLine -> nonNull(sharedResultLine.getOffenceId()) && sharedResultLine.getOffenceId().equals(offence.getId()) && OFFENCE.equalsIgnoreCase(sharedResultLine.getLevel()))
                .collect(Collectors.toList());
    }

    private static List<SharedResultLine> getCaseLevelSharedResults(final Defendant defendant, final ProsecutionCase prosecutionCase, final Set<SharedResultLine> sharedResultLines) {
        return sharedResultLines.stream()
                .filter(sharedResultLine -> nonNull(sharedResultLine.getProsecutionCaseId())
                        && sharedResultLine.getProsecutionCaseId().equals(prosecutionCase.getId())
                        && defendant.getId().equals(sharedResultLine.getDefendantId())
                        && !(OFFENCE.equalsIgnoreCase(sharedResultLine.getLevel())))
                .collect(Collectors.toList());
    }

    private static String getDefendantAge(final uk.gov.justice.core.courts.Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            final String dateOfBirthText = defendant.getPersonDefendant().getPersonDetails().getDateOfBirth();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            final LocalDate dateOfBirth = LocalDate.parse(dateOfBirthText, formatter);
            final LocalDate now = LocalDate.now();
            return Integer.toString(now.getYear() - dateOfBirth.getYear());
        }
        return EMPTY;
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
        final StringBuilder nameBuilder = new StringBuilder();
        if (nonNull(defendant.getPersonDefendant())) {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            final Person personDetails = personDefendant.getPersonDetails();
            if (nonNull(personDetails.getFirstName())) {
                nameBuilder.append(personDetails.getFirstName());
            }
            if (nonNull(personDetails.getMiddleName())) {
                if (!nameBuilder.toString().isEmpty()) {
                    nameBuilder.append(SPACE);
                }
                nameBuilder.append(personDetails.getMiddleName());
            }
            nameBuilder.append(SPACE + personDetails.getLastName());
        }
        return nameBuilder.toString();
    }
}
