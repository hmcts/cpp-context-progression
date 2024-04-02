package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static java.lang.Boolean.TRUE;
import static java.lang.System.lineSeparator;
import static java.time.Period.between;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictionsForOffence;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OnlinePleasAllocation;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.moj.cpp.progression.plea.json.schemas.OpaNoticeDocument;
import uk.gov.moj.cpp.progression.plea.json.schemas.OpaOffence;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class OpaNoticeHelper {

    private static final String UNCONDITIONAL_CODE = "U";
    private static final String DESC_CONDITIONAL = "Conditional";
    private static final String DESC_UNCONDITIONAL = "Unconditional";
    private static final String YOUTH = "Youth";

    private OpaNoticeHelper() {
    }

    public static OpaNoticeDocument generateOpaPublicListNotice(final UUID caseId,
                                                                final UUID defendantId,
                                                                final Hearing hearing,
                                                                final Prosecutor prosecutor) {
        final boolean welshCourt = isWelshCourt(hearing);
        final OpaNoticeDocument.Builder builder = OpaNoticeDocument.opaNoticeDocument();

        getProsecutionCase(hearing, caseId).ifPresent(prosecutionCase -> populateCaseUrn(prosecutionCase.getProsecutionCaseIdentifier(), builder));
        getFirstHearingDate(hearing).ifPresent(builder::withFirstHearingDate);
        populateProsecutor(prosecutor, builder);
        getDefendant(hearing, defendantId).ifPresent(defendant -> {
            populateDependantFullName(defendant, builder, false);
            populateOffenceDetails(defendant, builder, welshCourt);
        });

        return builder.build();
    }

    public static OpaNoticeDocument generateOpaPressListNotice(final UUID caseId,
                                                               final UUID defendantId,
                                                               final Hearing hearing,
                                                               final Prosecutor prosecutor,
                                                               final OnlinePleasAllocation pleasAllocation) {
        final boolean welshCourt = isWelshCourt(hearing);
        final OpaNoticeDocument.Builder builder = OpaNoticeDocument.opaNoticeDocument();

        populateProsecutor(prosecutor, builder);
        getProsecutionCase(hearing, caseId).ifPresent(prosecutionCase -> populateCaseUrn(prosecutionCase.getProsecutionCaseIdentifier(), builder));
        getFirstHearingDate(hearing).ifPresent(builder::withFirstHearingDate);
        getDefendant(hearing, defendantId).ifPresent(defendant -> {
            builder.withYouth(TRUE.equals(defendant.getIsYouth()) ? YOUTH : EMPTY);
            populateDependantFullName(defendant, builder, true);
            populateFullOffenceDetails(defendant, pleasAllocation, builder, welshCourt);
            populateDefendantAge(defendant.getPersonDefendant(), hearing, builder);
        });

        return builder.build();
    }

    public static OpaNoticeDocument generateOpaResultListNotice(final UUID caseId,
                                                                final UUID defendantId,
                                                                final Hearing hearing) {
        final boolean welshCourt = isWelshCourt(hearing);
        final OpaNoticeDocument.Builder builder = OpaNoticeDocument.opaNoticeDocument();

        getProsecutionCase(hearing, caseId).ifPresent(prosecutionCase -> populateCaseUrn(prosecutionCase.getProsecutionCaseIdentifier(), builder));
        getFirstHearingDate(hearing).ifPresent(builder::withFirstHearingDate);
        populateHearingDateAndCourtName(hearing, builder, welshCourt);
        getDefendant(hearing, defendantId).ifPresent(defendant -> {
            populateDependantFullName(defendant, builder, false);
            populateOffenceDetailsForResult(defendant, builder, welshCourt);
            populateBailStatus(defendant, builder);
        });

        return builder.build();
    }

    private static void populateOffenceDetailsForResult(final Defendant defendant,
                                                        final OpaNoticeDocument.Builder builder,
                                                        final boolean welshCourt) {
        final List<OpaOffence> offences = defendant.getOffences().stream()
                .map(offence -> getOffenceDetailForResult(welshCourt, offence))
                .collect(toList());

        builder.withOffences(offences);
    }

    private static OpaOffence getOffenceDetailForResult(final boolean welshCourt, final Offence offence) {
        final OpaOffence.Builder offenceBuilder = new OpaOffence.Builder();

        populateOffenceTitleAndLegislation(offence, offenceBuilder, welshCourt);
        populatePressRestrictions(offence, offenceBuilder);
        populateAllocationDecision(offence, offenceBuilder);
        populateNextHearingDetails(offence, offenceBuilder, welshCourt);

        return offenceBuilder.build();
    }

    private static void populateCaseUrn(final ProsecutionCaseIdentifier caseIdentifier, final OpaNoticeDocument.Builder documentBuilder) {
        ofNullable(caseIdentifier).ifPresent(ci -> documentBuilder.withCaseUrn(ci.getCaseURN()));
    }

    private static void populateBailStatus(final Defendant defendant, final OpaNoticeDocument.Builder builder) {
        ofNullable(defendant.getPersonDefendant()).flatMap(personDefendant -> ofNullable(personDefendant.getBailStatus()))
                .ifPresent(bailStatus -> {
                    final String bailDescription = UNCONDITIONAL_CODE.equals(bailStatus.getCode()) ? DESC_UNCONDITIONAL : DESC_CONDITIONAL;
                    builder.withBailStatus(bailDescription);
                });
    }

    private static void populateAllocationDecision(final Offence offence, final OpaOffence.Builder builder) {
        ofNullable(offence.getAllocationDecision()).ifPresent(
                allocationDecision ->
                        builder.withAllocationDecision(allocationDecision.getMotReasonDescription())
                                .withDecisionDate(allocationDecision.getAllocationDecisionDate()));
    }

    private static void populateHearingDateAndCourtName(final Hearing hearing,
                                                        final OpaNoticeDocument.Builder builder,
                                                        final boolean welshCourt) {
        getFirstHearingDate(hearing).ifPresent(builder::withFirstHearingDate);

        if (welshCourt) {
            ofNullable(hearing.getCourtCentre()).ifPresent(court ->
                    builder.withCourtId(court.getId())
                            .withCourt(court.getWelshName())
                            .withCourtAddress(court.getWelshAddress()));
        } else {
            ofNullable(hearing.getCourtCentre()).ifPresent(court ->
                    builder.withCourtId(court.getId())
                            .withCourt(court.getName())
                            .withCourtAddress(court.getAddress()));
        }
    }

    private static void populateNextHearingDetails(final Offence offence,
                                                   final OpaOffence.Builder builder,
                                                   final boolean welshCourt) {
        ofNullable(offence.getJudicialResults()).flatMap(OpaNoticeHelper::getNextHearing)
                .ifPresent(nextHearing -> getNextHearingLocation(builder, nextHearing, welshCourt));
    }

    private static void getNextHearingLocation(final OpaOffence.Builder builder,
                                               final NextHearing nextHearing,
                                               final boolean welshCourt) {
        if (welshCourt) {
            ofNullable(nextHearing.getCourtCentre()).map(CourtCentre::getWelshName)
                    .ifPresent(builder::withNextHearingLocation);
        } else {
            ofNullable(nextHearing.getCourtCentre()).map(CourtCentre::getName)
                    .ifPresent(builder::withNextHearingLocation);
        }

        builder.withNextHearingDate(nextHearing.getListedStartDateTime());
    }

    private static Optional<NextHearing> getNextHearing(final List<JudicialResult> judicialResults) {
        return judicialResults.stream()
                .map(JudicialResult::getNextHearing)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static void populateOffenceDetails(final Defendant defendant,
                                               final OpaNoticeDocument.Builder builder,
                                               final boolean welshCourt) {
        final List<OpaOffence> offences = defendant.getOffences().stream()
                .map(offence -> getOpaOffence(welshCourt, offence))
                .collect(toList());

        builder.withOffences(offences);
    }

    private static OpaOffence getOpaOffence(final boolean welshCourt, final Offence offence) {
        final OpaOffence.Builder offenceBuilder = new OpaOffence.Builder();

        populateOffenceTitleAndLegislation(offence, offenceBuilder, welshCourt);
        populatePressRestrictions(offence, offenceBuilder);

        return offenceBuilder.build();
    }

    private static void populateOffenceTitleAndLegislation(final Offence offence,
                                                           final OpaOffence.Builder offenceBuilder,
                                                           final boolean welshCourt) {
        if (welshCourt) {
            offenceBuilder.withTitle(offence.getOffenceTitleWelsh())
                    .withLegislation(offence.getOffenceLegislationWelsh());
        } else {
            offenceBuilder.withTitle(offence.getOffenceTitle())
                    .withLegislation(offence.getOffenceLegislation());
        }
    }

    private static void populateFullOffenceDetails(final Defendant defendant,
                                                   final OnlinePleasAllocation onlinePleasAllocation,
                                                   final OpaNoticeDocument.Builder documentBuilder,
                                                   final boolean welshCourt) {
        final List<OpaOffence> offences = defendant.getOffences().stream()
                .map(offence -> getOpaOffence(onlinePleasAllocation, welshCourt, offence))
                .collect(toList());

        documentBuilder.withOffences(offences);
    }

    private static OpaOffence getOpaOffence(final OnlinePleasAllocation opa,
                                            final boolean welshCourt,
                                            final Offence offence) {
        final OpaOffence.Builder builder = new OpaOffence.Builder();

        if (welshCourt) {
            builder.withTitle(offence.getOffenceTitleWelsh())
                    .withLegislation(offence.getOffenceLegislationWelsh())
                    .withOffenceDate(offence.getStartDate())
                    .withWording(offence.getWordingWelsh());
        } else {
            builder.withTitle(offence.getOffenceTitle())
                    .withLegislation(offence.getOffenceLegislation())
                    .withOffenceDate(offence.getStartDate())
                    .withWording(offence.getWording());
        }

        populatePleaDetails(offence.getId(), opa, builder);
        populatePressRestrictions(offence, builder);

        return builder.build();
    }

    private static void populatePleaDetails(final UUID offenceId,
                                            final OnlinePleasAllocation opa,
                                            final OpaOffence.Builder builder) {
        ofNullable(opa.getOffences())
                .flatMap(offences -> offences.stream()
                        .filter(offence -> offenceId.equals(offence.getOffenceId()))
                        .findFirst())
                .ifPresent(plea ->
                        builder.withIndicatedPlea(plea.getIndicatedPlea())
                                .withPleaStartDate(plea.getPleaDate()));
    }

    private static void populateDependantFullName(final Defendant defendant,
                                                  final OpaNoticeDocument.Builder builder,
                                                  final boolean fullDetail) {
        ofNullable(defendant.getPersonDefendant())
                .flatMap(personDefendant -> ofNullable(personDefendant.getPersonDetails()))
                .ifPresent(person -> populatePersonDetails(builder, person, fullDetail));

        ofNullable(defendant.getLegalEntityDefendant())
                .flatMap(legalEntity -> ofNullable(legalEntity.getOrganisation()))
                .ifPresent(org -> populateOrganisationDetails(builder, org, fullDetail));
    }

    private static void populateOrganisationDetails(final OpaNoticeDocument.Builder builder,
                                                    final Organisation org,
                                                    final boolean fullDetail) {
        builder.withOrganisationName(org.getName());

        if (fullDetail) {
            populateAddress(org.getAddress(), builder);
        }
    }

    private static void populatePersonDetails(final OpaNoticeDocument.Builder builder,
                                              final Person person,
                                              final boolean fullDetail) {
        builder.withFirstName(person.getFirstName())
                .withMiddleName(person.getMiddleName())
                .withLastName(person.getLastName());

        if (fullDetail) {
            builder.withDob(person.getDateOfBirth());
            populateAddress(person.getAddress(), builder);
        }
    }

    private static void populateAddress(final Address address,
                                        final OpaNoticeDocument.Builder builder) {
        ofNullable(address).ifPresent(builder::withDefendantAddress);
    }

    private static void populatePressRestrictions(final Offence offence,
                                                  final OpaOffence.Builder builder) {
        ofNullable(dedupAllReportingRestrictionsForOffence(offence)
                .getReportingRestrictions()).ifPresent(rr -> {
            final String reportRestrictions = rr.stream()
                    .map(ReportingRestriction::getLabel)
                    .distinct()
                    .collect(joining(lineSeparator()));

            builder.withReportRestrictions(reportRestrictions);
        });
    }

    private static void populateProsecutor(final Prosecutor prosecutor,
                                           final OpaNoticeDocument.Builder builder) {
        ofNullable(prosecutor).ifPresent(ps ->
                builder.withProsecutor(ps.getProsecutorName()));
    }

    private static void populateDefendantAge(final PersonDefendant personDefendant,
                                             final Hearing hearing,
                                             final OpaNoticeDocument.Builder builder) {
        getFirstHearingDate(hearing).ifPresent(hearingDate -> {
            final String age = ofNullable(personDefendant)
                    .map(PersonDefendant::getPersonDetails)
                    .map(Person::getDateOfBirth)
                    .map(dob -> between(dob, hearingDate.toLocalDate()))
                    .map(Period::getYears)
                    .map(String::valueOf).orElse(EMPTY);

            builder.withAge(age);
        });
    }

    private static Optional<ZonedDateTime> getFirstHearingDate(final Hearing hearing) {
        return hearing.getHearingDays()
                .stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst();
    }

    private static Optional<ProsecutionCase> getProsecutionCase(final Hearing hearing, final UUID caseId) {
        return hearing.getProsecutionCases()
                .stream()
                .filter(pc -> pc.getId().equals(caseId))
                .findFirst();
    }

    private static Optional<Defendant> getDefendant(final Hearing hearing, final UUID defendantId) {
        return hearing.getProsecutionCases()
                .stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantId)))
                .findFirst();
    }

    private static boolean isWelshCourt(final Hearing hearing) {
        return ofNullable(hearing.getCourtCentre())
                .map(CourtCentre::getWelshCourtCentre).orElse(false);
    }
}
