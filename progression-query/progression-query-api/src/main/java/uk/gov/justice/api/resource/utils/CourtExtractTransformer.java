package uk.gov.justice.api.resource.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.api.resource.DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.COURT_EXTRACT;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationOutcome;
import uk.gov.justice.core.courts.CourtApplicationOutcomeType;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationResponse;
import uk.gov.justice.core.courts.CourtApplicationResponseType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.ApplicantRepresentation;
import uk.gov.justice.progression.courts.exract.AttendanceDays;
import uk.gov.justice.progression.courts.exract.CompanyRepresentatives;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtDecisions;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.DefenceCounsels;
import uk.gov.justice.progression.courts.exract.DefenceOrganisations;
import uk.gov.justice.progression.courts.exract.Defendant;
import uk.gov.justice.progression.courts.exract.HearingDays;
import uk.gov.justice.progression.courts.exract.Judiciary;
import uk.gov.justice.progression.courts.exract.ParentGuardian;
import uk.gov.justice.progression.courts.exract.ProsecutionCounsels;
import uk.gov.justice.progression.courts.exract.PublishingCourt;
import uk.gov.justice.progression.courts.exract.Representation;
import uk.gov.justice.progression.courts.exract.RespondentRepresentation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

@SuppressWarnings({"squid:S1612", "squid:S3655", "squid:S2259", "squid:S1188", "squid:S2789", "squid:S1067","squid:MethodCyclomaticComplexity", "pmd:NullAssignment"})
public class CourtExtractTransformer {

    @Inject
    TransformationHelper transformationHelper;

    public CourtExtractRequested getCourtExtractRequested(final GetCaseAtAGlance caseAtAGlance, final String defendantId, final String extractType, final List<String> selectedHearingIdList, final UUID userId, final ProsecutionCase prosecutionCase) {
        final CourtExtractRequested.Builder courtExtract = CourtExtractRequested.courtExtractRequested();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        courtExtract.withExtractType(extractType);

        courtExtract.withCaseReference(getCaseReference(caseAtAGlance.getProsecutionCaseIdentifier()));

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
            latestHearing = transformationHelper.getLatestHearings(hearingsList);
        } else {
            latestHearing = hearingsList.get(0);
        }

        final Defendant defendant = transformDefendants(latestHearing.getDefendants(), defendantId, defendantBuilder, hearingsList);
        courtExtract.withDefendant(defendant);
        courtExtract.withPublishingCourt(transformCourtCentre(latestHearing.getCourtCentre(), userId));
        courtExtract.withProsecutingAuthority(transformationHelper.transformProsecutingAuthority(caseAtAGlance.getProsecutionCaseIdentifier(), userId));
        if (latestHearing.getProsecutionCounsels() != null) {
            courtExtract.withProsecutionCounsels(transformProsecutionCounsels(latestHearing.getProsecutionCounsels()));
        }

        if(!isEmpty(latestHearing.getCompanyRepresentatives())){
            courtExtract.withCompanyRepresentatives(transformCompanyRepresentatives(latestHearing.getCompanyRepresentatives()));
        }

        courtExtract.withCourtDecisions(transformCourtDecisions(hearingsList));

        //parentGuardian
        final Optional<uk.gov.justice.core.courts.Defendant> caseDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().toString().equals(defendantId))
                .findFirst();
        if (caseDefendant.isPresent() && caseDefendant.get().getAssociatedPersons() != null && !caseDefendant.get().getAssociatedPersons().isEmpty()) {
            courtExtract.withParentGuardian(transformParentGuardian(caseDefendant.get().getAssociatedPersons()));
        }

        //referralReason
        courtExtract.withReferralReason(getreferralReason(hearingsList));

        //courtApplications
        if (caseAtAGlance.getCourtApplications() != null && !caseAtAGlance.getCourtApplications().isEmpty()) {
            courtExtract.withCourtApplications(transformCourtApplications(caseAtAGlance.getCourtApplications(), hearingsList));
        }

        return courtExtract.build();
    }

    private String getreferralReason(final List<Hearings> hearingsList) {
        return hearingsList.stream()
                .filter(h -> nonNull(h.getDefendantReferralReasons()))
                .flatMap((h -> h.getDefendantReferralReasons().stream()))
                .filter(Objects::nonNull)
                .findAny()
                .map(ReferralReason::getDescription)
                .orElse(null);
    }

    private List<CourtApplications> transformCourtApplications(final List<CourtApplication> courtApplications,  final List<Hearings> hearingsList) {
        final List<CourtApplication> applicationsExtractList = new ArrayList<>();
        hearingsList.stream()
                .filter(Objects::nonNull)
                .filter(h -> nonNull(h.getDefendants()))
                .flatMap(h -> h.getDefendants().stream())
                .filter(Objects::nonNull)
                .filter(d -> nonNull(d.getCourtApplications()))
                .flatMap(d -> d.getCourtApplications().stream())
                .filter(Objects::nonNull)
                .forEach(resultedApplication -> buildExtractApplication(resultedApplication, courtApplications, applicationsExtractList));

        return applicationsExtractList.stream()
                .map(ca -> CourtApplications.courtApplications()
                        .withDecision(ca.getApplicationOutcome() != null ? ca.getApplicationOutcome().getApplicationOutcomeType().getDescription() : null)
                        .withDecisionDate(ca.getApplicationOutcome() != null ? ca.getApplicationOutcome().getApplicationOutcomeDate() : null)
                        .withResponse(transformationHelper.isApplicationResponseAvailable(ca.getRespondents()) ? transformationHelper.transformApplicationResponse(ca.getRespondents()) : null)
                        .withResponseDate(transformationHelper.isApplicationResponseAvailable(ca.getRespondents()) ? transformationHelper.transformApplicationResponseDate(ca.getRespondents()) : null)
                        .withResults(ca.getJudicialResults())
                        .withRepresentation(transformRepresentation(ca, hearingsList))
                        .withApplicationType(ca.getType().getApplicationType())
                        .build()).collect(Collectors.toList());
    }

    private void buildExtractApplication(uk.gov.justice.progression.courts.CourtApplications resultedApplication, List<CourtApplication>  origCourtApplicationsList, final List<CourtApplication> applicationExtractList){

        final List<UUID> applicationIds = applicationExtractList.stream().filter(Objects::nonNull).map(CourtApplication::getId).collect(Collectors.toList());
        final Optional<CourtApplication> existingMatchedApplication =
                origCourtApplicationsList.stream().filter(ca -> ca.getId().toString().equals(resultedApplication.getApplicationId().toString())).findFirst();
        if((applicationIds.isEmpty() && existingMatchedApplication.isPresent()) || (existingMatchedApplication.isPresent() && !applicationIds.contains(existingMatchedApplication.get().getId()))) {
                applicationExtractList.add(CourtApplication.courtApplication()
                        .withId(existingMatchedApplication.get().getId())
                        .withApplicationReference(existingMatchedApplication.get().getApplicationReference())
                        .withType(existingMatchedApplication.get().getType())
                        .withApplicant(existingMatchedApplication.get().getApplicant())
                        .withRespondents(updateResponse(existingMatchedApplication.get().getRespondents(), resultedApplication))
                        .withJudicialResults(resultedApplication.getJudicialResults())
                        .withApplicationOutcome(CourtApplicationOutcome.courtApplicationOutcome()
                                .withApplicationOutcomeType(CourtApplicationOutcomeType.courtApplicationOutcomeType().withDescription(resultedApplication.getOutcome()).build())
                                .withApplicationOutcomeDate(resultedApplication.getOutcomeDate())
                                .build())
                        .build());
        }
    }

    private List<CourtApplicationRespondent> updateResponse(final List<CourtApplicationRespondent> courtApplicationRespondents, final uk.gov.justice.progression.courts.CourtApplications resultedApplication) {
        final List<CourtApplicationRespondent> updatedResponseList = new ArrayList(courtApplicationRespondents);
        if(nonNull(resultedApplication.getRespondents()) && !resultedApplication.getRespondents().isEmpty()) {
            final String responseDescription = resultedApplication.getRespondents().stream()
                    .filter(Objects::nonNull)
                    .filter(r -> nonNull(r.getApplicationResponse()))
                    .map(r -> r.getApplicationResponse())
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElse(EMPTY);

            final LocalDate responseDate = resultedApplication.getRespondents().stream()
                    .filter(Objects::nonNull)
                    .filter(r -> nonNull(r.getApplicationResponse()))
                    .map(r -> r.getResponseDate())
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElse(null);

            updatedResponseList.add(CourtApplicationRespondent.courtApplicationRespondent()
                    .withApplicationResponse(CourtApplicationResponse.courtApplicationResponse()
                            .withApplicationResponseType(CourtApplicationResponseType.courtApplicationResponseType()
                                    .withDescription(responseDescription).build())
                            .withApplicationResponseDate(nonNull(responseDate) ? responseDate : null)
                    .build()).build());
        }
        return updatedResponseList;
    }

    private Representation transformRepresentation(final CourtApplication ca, final List<Hearings> hearingsList) {
        return Representation.representation()
                .withApplicantRepresentation(transformApplicantRepresentation(ca.getApplicant().getRepresentationOrganisation(), ca.getType().getApplicantSynonym(), hearingsList))
                .withRespondentRepresentation(ca.getRespondents() != null ? transformRespondantRepresentations(ca.getRespondents(), ca.getType().getRespondentSynonym(), hearingsList) : null)
                .build();
    }

    private List<RespondentRepresentation> transformRespondantRepresentations(final List<CourtApplicationRespondent> respondents, final String synonym, final List<Hearings> hearingsList) {
        final List<RespondentRepresentation> respondentRepresentations = new ArrayList<>();
        respondents.stream().forEach(r -> respondentRepresentations.add(transformRespondantRepresentation(r.getPartyDetails(), synonym, hearingsList)));
        return respondentRepresentations;
    }

    private RespondentRepresentation transformRespondantRepresentation(final CourtApplicationParty courtApplicationParty, final String synonym, final List<Hearings> hearingsList) {
        final Organisation representationOrganisation = nonNull(courtApplicationParty) ? courtApplicationParty.getRepresentationOrganisation() : null;
        return RespondentRepresentation.respondentRepresentation()
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress(): null)
                .withName(nonNull(representationOrganisation) ? representationOrganisation.getName() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withSynonym(synonym)
                .withRespondentCounsels(hearingsList.stream()
                        .filter(hearings -> hearings.getRespondentCounsels() != null)
                        .flatMap(hearings -> hearings.getRespondentCounsels().stream())
                        .collect(Collectors.toList()))
                .build();
    }

    private ApplicantRepresentation transformApplicantRepresentation(final Organisation representationOrganisation, final String synonym, final List<Hearings> hearingsList) {
        return ApplicantRepresentation.applicantRepresentation()
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withName(nonNull(representationOrganisation) ? representationOrganisation.getName() : null)
                .withSynonym(synonym)
                .withApplicantCounsels(hearingsList.stream()
                        .filter(hearings -> hearings.getApplicantCounsels() != null)
                        .flatMap(hearings -> hearings.getApplicantCounsels().stream())
                        .collect(Collectors.toList()))
                .build();
    }


    private ParentGuardian transformParentGuardian(final List<AssociatedPerson> associatedPersons) {
        final Optional<Person> person = associatedPersons.stream()
                .filter(associatedPerson -> nonNull(associatedPerson.getPerson()))
                .findFirst()
                .map(AssociatedPerson::getPerson);
        if (person.isPresent()) {
            return ParentGuardian.parentGuardian()
                    .withName(transformationHelper.getName(person.get().getFirstName(), person.get().getMiddleName(), person.get().getLastName()))
                    .withAddress(person.get().getAddress())
                    .build();
        }
        return null;
    }

    private List<CourtDecisions> transformCourtDecisions(final List<Hearings> hearings) {
        final List<CourtDecisions> courtDecisiones = new ArrayList<>();
        hearings.forEach(h -> {
            if (h.getJudiciary() != null) {
                final CourtDecisions courtDecisions = CourtDecisions.courtDecisions()
                        .withDates(transformationHelper.transformDates(h.getHearingDays()))
                        .withJudiciary(
                                h.getJudiciary().stream().map(j ->
                                        Judiciary.judiciary()
                                                .withIsDeputy(j.getIsDeputy())
                                                .withRole(j.getJudicialRoleType() != null ? j.getJudicialRoleType().toString() : null)
                                                .withName(transformationHelper.getName(j.getFirstName(), j.getMiddleName(), j.getLastName()))
                                                .withIsBenchChairman(j.getIsBenchChairman())
                                                .build()
                                ).collect(Collectors.toList()))
                        .withJudicialDisplayName(transformationHelper.transformJudicialDisplayName(h.getJudiciary()))
                        .withRoleDisplayName((!h.getJudiciary().isEmpty() && h.getJudiciary().get(0).getJudicialRoleType() != null) ? transformationHelper.getCamelCase(h.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()) : null)
                        .build();
                courtDecisiones.add(courtDecisions);
            }
        });
        return courtDecisiones;
    }


    private List<ProsecutionCounsels> transformProsecutionCounsels(final List<ProsecutionCounsel> prosecutionCounselsList) {
        return prosecutionCounselsList.stream().map(pc -> ProsecutionCounsels.prosecutionCounsels()
                .withName(transformationHelper.getName(pc.getFirstName(), pc.getMiddleName(), pc.getLastName()))
                .withAttendanceDays(transformAttendanceDay(pc.getAttendanceDays()))
                .withRole(pc.getStatus())
                .build()
        ).collect(Collectors.toList());
    }

    private List<DefenceCounsels> transformDefenceCounsels(final List<DefenceCounsel> defenceCounselList) {
        return defenceCounselList.stream().map(dc -> DefenceCounsels.defenceCounsels()
                .withName(transformationHelper.getName(dc.getFirstName(), dc.getMiddleName(), dc.getLastName()))
                .withAttendanceDays(dc.getAttendanceDays() != null ? transformAttendanceDay(dc.getAttendanceDays()) : new ArrayList<>())
                .withRole(dc.getStatus())
                .build()).collect(Collectors.toList());
    }

    private List<AttendanceDays> transformAttendanceDay(final List<LocalDate> attendanceDaysList) {
        return attendanceDaysList.stream().map(ad -> AttendanceDays.attendanceDays()
                .withDay(ad)
                .build()).collect(Collectors.toList());
    }



    private PublishingCourt transformCourtCentre(final CourtCentre courtCentre, final UUID userId) {
        return PublishingCourt.publishingCourt()
                .withName(courtCentre.getName())
                .withAddress(transformationHelper.getCourtAddress(userId, courtCentre.getId()))
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

            defendantBuilder.withAttendanceDays(transformAttendanceDay( transformDefendantAttendanceDay(hearingsList, defendantId)));
            defendantBuilder.withResults(defendant.getJudicialResults());
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
        hearingsList.forEach(h -> {
            final Optional<Defendants> defendants = h.getDefendants().stream().filter(d -> d.getId().toString().equals(defendantId)).findFirst();
            if (defendants.isPresent()) {
                offences.addAll(transformOffence(defendants.get().getOffences()));
            }
        });
        return mergeOffencesAndResults(offences);
    }

    private List<LocalDate> transformDefendantAttendanceDay(final List<Hearings> hearingsList, final String defendantId) {
        return hearingsList.stream()
                .filter(h -> nonNull(h.getDefendantAttendance()))
                .flatMap((h -> h.getDefendantAttendance().stream()))
                .filter(Objects::nonNull)
                .filter( da -> da.getDefendantId().toString().equals(defendantId))
                .map(defendantAttendance -> defendantAttendance.getAttendanceDays())
                .flatMap(ab -> ab.stream())
                .filter(attendanceDay -> attendanceDay.getIsInAttendance())
                .map(attendanceDay -> attendanceDay.getDay())
                .collect(Collectors.toList());

    }

    private List<uk.gov.justice.progression.courts.exract.Offences> mergeOffencesAndResults(final List<uk.gov.justice.progression.courts.exract.Offences> offences) {
        final Map<UUID, uk.gov.justice.progression.courts.exract.Offences> offenceMap = new HashMap<>();
        offences.forEach(offence -> {
            if (offenceMap.get(offence.getId()) == null) {
                offenceMap.put(offence.getId(), offence);
            }
        });
        return new ArrayList<>(offenceMap.values());

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
                .withResults(o.getJudicialResults())
                .withNotifiedPlea(o.getNotifiedPlea())
                .withWording(o.getWording())
                .withPleas(o.getPleas())
                .withVerdicts(o.getVerdicts())
                .withWordingWelsh(o.getWordingWelsh()).build()

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
                        .withType(h.getType().getDescription()).build()
        ).collect(Collectors.toList());
    }

    private uk.gov.justice.progression.courts.exract.CourtCentre transformCourtCenter(final CourtCentre courtCentre) {
        return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                .withName(courtCentre.getName())
                .withId(courtCentre.getId())
                .withWelshName(courtCentre.getWelshName())
                .build();
    }

    private List<HearingDays> transformHearingDays(final List<HearingDay> hearingDaysList) {
        if (hearingDaysList.size() > 2) {
            return getToAndFromHearingDays(hearingDaysList);
        }
        return hearingDaysList.stream().map(hd ->
                HearingDays.hearingDays()
                        .withDay(hd.getSittingDay().toLocalDate())
                        .build()
        ).collect(Collectors.toList());
    }

    private List<HearingDays> getToAndFromHearingDays(final List<HearingDay> hearingDaysList) {
        final List<HearingDays> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDays.hearingDays()
                .withDay(hearingDaysList.get(0).getSittingDay().toLocalDate())
                .build());
        hearingDays.add(HearingDays.hearingDays()
                .withDay(hearingDaysList.get(hearingDaysList.size() - 1).getSittingDay().toLocalDate())
                .build());
        return hearingDays;
    }

    private String getCaseReference(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return prosecutionCaseIdentifier.getCaseURN() != null ? prosecutionCaseIdentifier.getCaseURN() : prosecutionCaseIdentifier.getProsecutionAuthorityReference();
    }

    private List<CompanyRepresentatives> transformCompanyRepresentatives(final List<CompanyRepresentative> companyRepresentatives) {
        return companyRepresentatives.stream().map(cr -> CompanyRepresentatives.companyRepresentatives()
                .withName(transformationHelper.getName(cr.getFirstName(),EMPTY,cr.getLastName()))
                .withAttendanceDays(transformAttendanceDay(cr.getAttendanceDays()))
                .withRole(cr.getPosition() != null ? cr.getPosition().toString() : EMPTY)
                .build()
        ).collect(Collectors.toList());
    }
}
