package uk.gov.justice.api.resource.utils;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.api.resource.DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.COURT_EXTRACT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ResultPrompt;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.AttendanceDays;
import uk.gov.justice.progression.courts.exract.CourtDecisions;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.Dates;
import uk.gov.justice.progression.courts.exract.DefenceCounsels;
import uk.gov.justice.progression.courts.exract.DefenceOrganisations;
import uk.gov.justice.progression.courts.exract.Defendant;
import uk.gov.justice.progression.courts.exract.HearingDays;
import uk.gov.justice.progression.courts.exract.Judiciary;
import uk.gov.justice.progression.courts.exract.Prompts;
import uk.gov.justice.progression.courts.exract.ProsecutingAuthority;
import uk.gov.justice.progression.courts.exract.ProsecutionCounsels;
import uk.gov.justice.progression.courts.exract.PublishingCourt;
import uk.gov.justice.progression.courts.exract.Results;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.CaseFormat;

@SuppressWarnings({"squid:S3655", "squid:S2259", "squid:S1188", "squid:S2789"})
public class CourtExtractTransformer {

    @Inject
    ReferenceDataService referenceDataService;

    private int winger = 1;


    public CourtExtractRequested getCourtExtractRequested(final GetCaseAtAGlance caseAtAGlance, final String defendantId, final String extractType, final List<String> selectedHearingIdList, final UUID userId) {
        final CourtExtractRequested.Builder builder = CourtExtractRequested.courtExtractRequested();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        builder.withExtractType(extractType);

        builder.withCaseReference(getCaseReference(caseAtAGlance.getProsecutionCaseIdentifier()));

        final DefendantHearings defendantHearings = caseAtAGlance.getDefendantHearings().stream().filter(dh -> dh.getDefendantId().toString().equals(defendantId)).findFirst().get();
        if (nonNull(defendantHearings.getDefendantName())) {
            defendantBuilder.withName(defendantHearings.getDefendantName());
        }
        if (nonNull(defendantHearings.getDefendantId())) {
            defendantBuilder.withId(defendantHearings.getDefendantId());
        }
        final List<UUID> hearingIds = defendantHearings.getHearingIds();

        //latest hearing for defendant
        Hearings latestHearing;

        final List<Hearings> hearingsList = caseAtAGlance.getHearings().stream()
                .filter(h -> hearingIds.contains(h.getId()))
                .filter(COURT_EXTRACT.equals(extractType) ? h -> selectedHearingIdList.contains(h.getId().toString()) : h -> true)
                .collect(Collectors.toList());

        if (hearingsList.size() > 1) {
            latestHearing = getLatestHearing(hearingsList);
        } else {
            latestHearing = hearingsList.get(0);
        }

        final Defendant defendant = transformDefendants(latestHearing.getDefendants(), defendantId, defendantBuilder, hearingsList);
        builder.withDefendant(defendant);
        builder.withPublishingCourt(transformCourtCentre(latestHearing.getCourtCentre(), userId));
        builder.withProsecutingAuthority(transformProsecutingAuthority(caseAtAGlance.getProsecutionCaseIdentifier(), userId));
        if (latestHearing.getProsecutionCounsels() != null) {
            builder.withProsecutionCounsels(transformProsecutionCounsels(latestHearing.getProsecutionCounsels()));
        }
        builder.withCourtDecisions(transformCourtDecisions(hearingsList));
        return builder.build();
    }

    private List<CourtDecisions> transformCourtDecisions(final List<Hearings> hearings) {
        final List<CourtDecisions> courtDecisiones = new ArrayList<>();
        hearings.stream().forEach(h -> {
            if (h.getJudiciary() != null) {
                final CourtDecisions courtDecisions = CourtDecisions.courtDecisions()
                        .withDates(transformDates(h.getHearingDays()))
                        .withJudiciary(
                                h.getJudiciary().stream().map(j ->
                                        Judiciary.judiciary()
                                                .withIsDeputy(j.getIsDeputy())
                                                .withRole(j.getJudicialRoleType() != null ? j.getJudicialRoleType().toString() : null)
                                                .withName(getName(j.getFirstName(), j.getMiddleName(), j.getLastName()))
                                                .withIsBenchChairman(j.getIsBenchChairman())
                                                .build()
                                ).collect(Collectors.toList()))
                        .withJudicialDisplayName(transformJudicialDisplayName(h.getJudiciary()))
                        .withRoleDisplayName((!h.getJudiciary().isEmpty() && h.getJudiciary().get(0).getJudicialRoleType() != null) ? getCamelCase(h.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()) : null)
                        .build();
                courtDecisiones.add(courtDecisions);
            }
        });
        return courtDecisiones;
    }

    private int getWingerAndIncrement() {
        return winger++;
    }

    //   If isBenchChairman= true then display name should be Chair: name, if isDeputy= true then Winger1: name
    //   i.e. “judicialDisplayName”: “Chair: Elizabeth Cole, Winger1: Sharon Reed-Jones, Winger2: Greg Walsh”
    private String transformJudicialDisplayName(final List<JudicialRole> judicialRoles) {
        final StringBuilder sb = new StringBuilder();
        winger = 1;
        judicialRoles.forEach(j -> {

            if (nonNull(j.getIsBenchChairman()) && j.getIsBenchChairman()) {
                sb.append("Chair: ");
            } else if (nonNull(j.getIsDeputy()) && j.getIsDeputy()) {
                sb.append("Winger");
                sb.append(getWingerAndIncrement());
                sb.append(": ");
            }
            sb.append(getName(j.getFirstName(), j.getMiddleName(), j.getLastName()));
            sb.append(" ");

        });
        return sb.toString().trim();
    }

    private String getCamelCase(final String value) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value);
    }

    private List<ProsecutionCounsels> transformProsecutionCounsels(final List<ProsecutionCounsel> prosecutionCounselsList) {
        return prosecutionCounselsList.stream().map(pc -> ProsecutionCounsels.prosecutionCounsels()
                .withName(getName(pc.getFirstName(), pc.getMiddleName(), pc.getLastName()))
                .withAttendanceDays(transformAttendanceDay(pc.getAttendanceDays()))
                .withRole(pc.getStatus())
                .build()
        ).collect(Collectors.toList());
    }

    private List<DefenceCounsels> transformDefenceCounsels(final List<DefenceCounsel> defenceCounselList) {
        return defenceCounselList.stream().map(dc -> DefenceCounsels.defenceCounsels()
                .withName(getName(dc.getFirstName(), dc.getMiddleName(), dc.getLastName()))
                .withAttendanceDays(dc.getAttendanceDays() != null ? transformAttendanceDay(dc.getAttendanceDays()) : new ArrayList<>())
                .withRole(dc.getStatus())
                .build()).collect(Collectors.toList());
    }

    private List<AttendanceDays> transformAttendanceDay(final List<String> attendanceDaysList) {
        return attendanceDaysList.stream().map(ad -> AttendanceDays.attendanceDays()
                .withDay(ad)
                .build()).collect(Collectors.toList());
    }

    private String getName(final String firstName, final String middleName, final String lastName) {
        final StringBuilder sb = new StringBuilder();
        if (nonNull(firstName)) {
            sb.append(firstName);
            sb.append(" ");
        }
        if (nonNull(middleName)) {
            sb.append(middleName);
            sb.append(" ");
        }
        if (nonNull(lastName)) {
            sb.append(lastName);
        }
        return sb.toString().trim();
    }

    private PublishingCourt transformCourtCentre(final CourtCentre courtCentre, final UUID userId) {
        return PublishingCourt.publishingCourt()
                .withName(courtCentre.getName())
                .withAddress(getCourtAddress(userId, courtCentre.getId()))
                .build();
    }


    private Defendant transformDefendants(final List<Defendants> defendantsList, final String defendantId, final Defendant.Builder defendantBuilder, final List<Hearings> hearingsList) {
        final Optional<Defendants> defendants = defendantsList.stream().filter(d -> d.getId().toString().equals(defendantId)).findFirst();
        if (defendants.isPresent()) {
            final Defendants defendant = defendants.get();
            defendantBuilder.withDateOfBirth(defendant.getDateOfBirth());
            defendantBuilder.withAge(defendant.getAge());
            defendantBuilder.withAddress(
                    Address.address()
                            .withAddress1(defendant.getAddress().getAddress1())
                            .withAddress2(defendant.getAddress().getAddress2())
                            .withAddress3(defendant.getAddress().getAddress3())
                            .withAddress4(defendant.getAddress().getAddress4())
                            .withAddress5(defendant.getAddress().getAddress5())
                            .withPostcode(defendant.getAddress().getPostcode())
                            .build()
            );
            defendantBuilder.withHearings(transformHearing(hearingsList));
            final List<Results> results = transformResults(hearingsList, defendantId);
            if (!results.isEmpty()) {
                defendantBuilder.withResults(results);
            }
            final List<uk.gov.justice.progression.courts.exract.Offences> offences = transformOffence(hearingsList, defendantId);
            if (!offences.isEmpty()) {
                defendantBuilder.withOffences(offences);
            }
            if (nonNull(defendant.getDefenceOrganisation())) {
                defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(defendant.getDefenceOrganisation()));
            }
        }

        return defendantBuilder.build();
    }

    private List<DefenceOrganisations> transformDefenceOrganisation(final DefenceOrganisation defenceOrganisation) {
        return Arrays.asList(
                DefenceOrganisations.defenceOrganisations()
                        .withDefendantId(defenceOrganisation.getDefendantId())
                        .withDefenceCounsels(transformDefenceCounsels(defenceOrganisation.getDefenceCounsels()))
                        .withDefenceOrganisation(defenceOrganisation.getDefenceOrganisation())
                        .build()
        );
    }


    private List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final List<Hearings> hearingsList, final String defendantId) {
        final List<uk.gov.justice.progression.courts.exract.Offences> offences = new ArrayList<>();
        hearingsList.stream().forEach(h -> {
            final Optional<Defendants> defendants = h.getDefendants().stream().filter(d -> d.getId().toString().equals(defendantId)).findFirst();
            if (defendants.isPresent()) {
                offences.addAll(transformOffence(defendants.get().getOffences()));
            }
        });
        return mergeOffencesAndResults(offences);
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> mergeOffencesAndResults(final List<uk.gov.justice.progression.courts.exract.Offences> offences) {
        final Map<UUID, uk.gov.justice.progression.courts.exract.Offences> offenceMap = new HashMap<>();
        offences.forEach(offence -> {
            if (offenceMap.get(offence.getId()) == null) {
                offenceMap.put(offence.getId(), offence);
            } else {
                //if offence is repeating then merge offence results and remove duplicate results
                offenceMap.get(offence.getId()).getResults().addAll(offence.getResults());
                removeDuplicateResults(offenceMap.get(offence.getId()).getResults());
            }
        });
        return new ArrayList<>(offenceMap.values());

    }

    private void removeDuplicateResults(final List<Results> results) {
        final Set<UUID> resultsIds = new HashSet<>();
        results.stream()
                .filter(result -> resultsIds.add(result.getId()))
                .collect(Collectors.toList());
    }


    private List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final List<Offences> offences) {

        return offences.stream().map(o -> uk.gov.justice.progression.courts.exract.Offences.offences()
                .withId(o.getId())
                .withConvictionDate(o.getConvictionDate())
                .withCount(o.getCount())
                .withEndDate(o.getEndDate())
                .withIndicatedPlea(o.getIndicatedPlea())
                .withStartDate(o.getStartDate())
                .withOffenceDefinitionId(o.getOffenceDefinitionId())
                .withOffenceLegislation(o.getOffenceLegislation())
                .withOffenceLegislationWelsh(o.getOffenceLegislationWelsh())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceTitle(o.getOffenceTitle())
                .withOffenceTitleWelsh(o.getOffenceTitleWelsh())
                .withResults(o.getResults() != null ? transformResults(o.getResults()) : new ArrayList<>())
                .withNotifiedPlea(o.getNotifiedPlea())
                .withWording(o.getWording())
                .withPleas(o.getPleas())
                .withVerdicts(o.getVerdicts())
                .withWordingWelsh(o.getWordingWelsh()).build()

        ).collect(Collectors.toList());
    }

    private List<Results> transformResults(final List<Hearings> hearingsList, final String defendantId) {
        final List<Results> results = new ArrayList<>();
        hearingsList.stream().forEach(h -> {
            final Optional<Defendants> defendants = h.getDefendants().stream().filter(d -> d.getId().toString().equals(defendantId)).findFirst();
            if (defendants.isPresent()) {
                results.addAll(transformResults(defendants.get().getResults()));
            }
        });
        return results;
    }


    private List<Results> transformResults(final List<SharedResultLine> sharedResultLines) {
        return sharedResultLines.stream().map(rl -> Results.results()
                .withIsAvailableForCourtExtract(rl.getIsAvailableForCourtExtract())
                .withLabel(rl.getLabel())
                .withLevel(rl.getLevel())
                .withId(rl.getId())
                .withDelegatedPowers(rl.getDelegatedPowers())
                .withPrompts(rl.getPrompts() != null ? tranformPrompts(rl.getPrompts()) : new ArrayList<Prompts>())
                .withOrderedDate(rl.getOrderedDate())
                .withRank(rl.getRank())
                .withWelshLabel(rl.getWelshLabel())
                .build()).collect(Collectors.toList());
    }

    private List<Prompts> tranformPrompts(final List<ResultPrompt> prompts) {
        return prompts.stream().map(p ->
                Prompts.prompts()
                        .withId(p.getId())
                        .withValue(p.getValue())
                        .withLabel(p.getLabel())
                        .withIsAvailableForCourtExtract(p.getIsAvailableForCourtExtract())
                        .withFixedListCode(p.getFixedListCode())
                        .withWelshLabel(p.getWelshLabel())
                        .withPromptSequence(p.getPromptSequence())
                        .withWelshValue(p.getWelshValue())
                        .build()
        ).collect(Collectors.toList());
    }

    private List<uk.gov.justice.progression.courts.exract.Hearings> transformHearing(final List<Hearings> hearingsList) {
        return hearingsList.stream().map(h ->
                uk.gov.justice.progression.courts.exract.Hearings.hearings()
                        .withHearingDays(transformHearingDays(h.getHearingDays()))
                        .withId(h.getId())
                        .withJurisdictionType(h.getJurisdictionType() != null ? uk.gov.justice.progression.courts.exract.JurisdictionType.valueOf(h.getJurisdictionType().toString()) : null)
                        .withCourtCentre(transformCourtCenter(h.getCourtCentre()))
                        .withReportingRestrictionReason(h.getReportingRestrictionReason())
                        .withType(h.getType()).build()
        ).collect(Collectors.toList());
    }

    private uk.gov.justice.progression.courts.exract.CourtCentre transformCourtCenter(final CourtCentre courtCentre) {
        return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                .withName(courtCentre.getName())
                .withId(courtCentre.getId())
                .withWelshName(courtCentre.getWelshName())
                .build();
    }

    private List<Dates> transformDates(final List<HearingDay> hearingDaysList) {
        if (hearingDaysList.size() > 2) {
            return getToAndFromDays(hearingDaysList);
        }
        return hearingDaysList.stream().map(hday ->
                Dates.dates().withDay(hday.getSittingDay().toString()).build()
        ).collect(Collectors.toList());
    }

    private List<Dates> getToAndFromDays(final List<HearingDay> hearingDaysList) {
        final List<Dates> dates = new ArrayList<>();
        dates.add(Dates.dates()
                .withDay(hearingDaysList.get(0).getSittingDay().toString())
                .build());
        dates.add(Dates.dates()
                .withDay(hearingDaysList.get(hearingDaysList.size() - 1).getSittingDay().toString())
                .build());
        return dates;
    }

    private List<HearingDays> transformHearingDays(final List<HearingDay> hearingDaysList) {
        if (hearingDaysList.size() > 2) {
            return getToAndFromHearingDays(hearingDaysList);
        }
        return hearingDaysList.stream().map(hd ->
                HearingDays.hearingDays()
                        .withDay(hd.getSittingDay().toString())
                        .build()
        ).collect(Collectors.toList());
    }

    private List<HearingDays> getToAndFromHearingDays(final List<HearingDay> hearingDaysList) {
        final List<HearingDays> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDays.hearingDays()
                .withDay(hearingDaysList.get(0).getSittingDay().toString())
                .build());
        hearingDays.add(HearingDays.hearingDays()
                .withDay(hearingDaysList.get(hearingDaysList.size() - 1).getSittingDay().toString())
                .build());
        return hearingDays;
    }


    private Address getCourtAddress(final UUID userId, final UUID courtCentreId) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("referencedata.query.organisation-unit")
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );

        return referenceDataService.getCourtCentreAddress(jsonEnvelope, courtCentreId);
    }

    private ProsecutingAuthority transformProsecutingAuthority(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final UUID userId) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("referencedata.query.get-prosecutor")
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("id", prosecutionCaseIdentifier.getProsecutionAuthorityId().toString())
                        .build()
        );
        return referenceDataService.getProsecutor(jsonEnvelope, prosecutionCaseIdentifier.getProsecutionAuthorityId().toString());
    }

    private String getCaseReference(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return prosecutionCaseIdentifier.getCaseURN() != null ? prosecutionCaseIdentifier.getCaseURN() : prosecutionCaseIdentifier.getProsecutionAuthorityReference();
    }

    private Hearings getLatestHearing(final List<Hearings> hearingsList) {
        return hearingsList.stream().sorted(Comparator.comparing(h -> getSittingDay(h.getHearingDays()))).reduce((first, second) -> second).orElse(null);
    }

    private ZonedDateTime getSittingDay(final List<HearingDay> hearingDays) {
        return hearingDays.stream().sorted(Comparator.comparing(HearingDay::getSittingDay).reversed()).findFirst().get().getSittingDay();
    }



}
