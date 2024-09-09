package uk.gov.justice.api.resource.utils;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.api.resource.DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.COURT_EXTRACT;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.ApplicantRepresentation;
import uk.gov.justice.progression.courts.exract.AttendanceDayAndType;
import uk.gov.justice.progression.courts.exract.AttendanceDays;
import uk.gov.justice.progression.courts.exract.CompanyRepresentatives;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtDecisions;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CustodialEstablishment;
import uk.gov.justice.progression.courts.exract.DefenceCounsels;
import uk.gov.justice.progression.courts.exract.DefenceOrganisations;
import uk.gov.justice.progression.courts.exract.Defendant;
import uk.gov.justice.progression.courts.exract.Judiciary;
import uk.gov.justice.progression.courts.exract.ParentGuardian;
import uk.gov.justice.progression.courts.exract.ProsecutionCounsels;
import uk.gov.justice.progression.courts.exract.PublishingCourt;
import uk.gov.justice.progression.courts.exract.Representation;
import uk.gov.justice.progression.courts.exract.RespondentRepresentation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3457", "squid:S1612", "squid:S3655", "squid:S2259", "squid:S1188", "squid:S2789", "squid:S1067", "squid:MethodCyclomaticComplexity", "pmd:NullAssignment", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod", "squid:S1172"})
public class CourtExtractTransformer {

    public static final String NOT_PRESENT = "Not present";
    public static final String PRESENT_BY_PRISON_VIDEO_LINK = "Present - prison video link";
    public static final String PRESENT_BY_POLICE_VIDEO_LINK = "Present - police video link";
    public static final String PRESENT_IN_PERSON = "Present - in person";
    public static final String PRESENT_BY_VIDEO_DEFAULT = "Present - by video";
    private static final BiPredicate<Hearings, UUID> hearingsDefendantIdBiPredicate = (hearings, defendantId) -> nonNull(hearings.getYouthCourtDefendantIds()) && hearings.getYouthCourtDefendantIds().stream().anyMatch(youthDefendantId -> youthDefendantId.equals(defendantId));

    @Inject
    TransformationHelper transformationHelper;

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtExtractTransformer.class);

    public CourtExtractRequested getCourtExtractRequested(final GetHearingsAtAGlance hearingsAtAGlance, final String defendantId, final String extractType, final List<String> selectedHearingIdList, final UUID userId, final ProsecutionCase prosecutionCase) {
        final CourtExtractRequested.Builder courtExtract = CourtExtractRequested.courtExtractRequested();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        final Optional<uk.gov.justice.core.courts.Defendant> caseDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().toString().equals(defendantId)).findFirst();
        final Optional<UUID> masterDefendantId = caseDefendant.map(uk.gov.justice.core.courts.Defendant::getMasterDefendantId);

        courtExtract.withExtractType(extractType);
        courtExtract.withCaseReference(transformationHelper.getCaseReference(hearingsAtAGlance.getProsecutionCaseIdentifier()));

        if (caseDefendant.isPresent()) {
            final List<AssociatedPerson> associatedPersons = caseDefendant.get().getAssociatedPersons();
            if (nonNull(associatedPersons) && !associatedPersons.isEmpty()) {
                courtExtract.withParentGuardian(transformParentGuardian(associatedPersons)); //parentGuardian
            }
        }

        final DefendantHearings defendantHearings = hearingsAtAGlance.getDefendantHearings().stream().filter(dh -> dh.getDefendantId().toString().equals(defendantId)).findFirst().orElse(null);

        defendantBuilder.withName(nonNull(defendantHearings) && nonNull(defendantHearings.getDefendantName()) ? defendantHearings.getDefendantName() : EMPTY);
        defendantBuilder.withId(nonNull(defendantHearings) && nonNull(defendantHearings.getDefendantId()) ? defendantHearings.getDefendantId() : fromString(EMPTY));

        final List<UUID> hearingIds = defendantHearings.getHearingIds();
        final List<Hearings> hearingsList = hearingsAtAGlance.getHearings().stream()
                .filter(h -> hearingIds.contains(h.getId()))
                .filter(COURT_EXTRACT.equals(extractType) ? h -> selectedHearingIdList.contains(h.getId().toString()) : h -> true)
                .collect(Collectors.toList());

        if (caseDefendant.isPresent() && masterDefendantId.isPresent()) {
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, courtExtract, defendantBuilder, hearingsList, caseDefendant.get());
        }

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            courtExtract.withCourtApplications(transformCourtApplications(hearingsAtAGlance.getCourtApplications(), hearingsList));
            courtExtract.withIsAppealPending(transformationHelper.getAppealPendingFlag(hearingsAtAGlance.getCourtApplications()));
        }

        return courtExtract.build();
    }

    public CourtExtractRequested ejectCase(final ProsecutionCase prosecutionCase, final GetHearingsAtAGlance hearingsAtAGlance, final String defendantId, final UUID userId) {
        final CourtExtractRequested.Builder ejectExtract = CourtExtractRequested.courtExtractRequested();
        final Defendant.Builder defendantBuilder = Defendant.defendant();

        final Optional<uk.gov.justice.core.courts.Defendant> caseDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().toString().equals(defendantId)).findFirst();

        final DefendantHearings defendantHearings = hearingsAtAGlance.getDefendantHearings().stream()
                .filter(dh -> dh.getDefendantId().toString().equals(defendantId)).findFirst().get();

        final List<Hearings> hearingsList = hearingsAtAGlance.getHearings().stream()
                .filter(h -> defendantHearings.getHearingIds().contains(h.getId())).collect(Collectors.toList());

        if (caseDefendant.isPresent()) {
            defendantBuilder.withId(caseDefendant.get().getId());
            if(nonNull(caseDefendant.get().getPersonDefendant())) {
                defendantBuilder.withName(transformationHelper.getPersonName(caseDefendant.get().getPersonDefendant().getPersonDetails()));

                if (Objects.nonNull(caseDefendant.get().getPersonDefendant().getCustodialEstablishment())) {
                    defendantBuilder.withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                            .withCustody(caseDefendant.get().getPersonDefendant().getCustodialEstablishment().getCustody())
                            .withId(caseDefendant.get().getPersonDefendant().getCustodialEstablishment().getId())
                            .withName(caseDefendant.get().getPersonDefendant().getCustodialEstablishment().getName())
                            .build());
                }
            }
            else if(nonNull(caseDefendant.get().getLegalEntityDefendant())) {
                defendantBuilder.withName(caseDefendant.get().getLegalEntityDefendant().getOrganisation().getName());
                defendantBuilder.withAddress(caseDefendant.get().getLegalEntityDefendant().getOrganisation().getAddress());
            }

            if (caseDefendant.get().getAssociatedPersons() != null && !caseDefendant.get().getAssociatedPersons().isEmpty()) {
                ejectExtract.withParentGuardian(transformParentGuardian(caseDefendant.get().getAssociatedPersons()));
            }
        }

        ejectExtract.withExtractType(COURT_EXTRACT);
        ejectExtract.withCaseReference(transformationHelper.getCaseReference(prosecutionCase.getProsecutionCaseIdentifier()));

        LOGGER.info("Hearings {}", isNotEmpty(defendantHearings.getHearingIds()) ? defendantHearings.getHearingIds() : "No hearings present");

        if (isNotEmpty(hearingsList)) {
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, ejectExtract, defendantBuilder, hearingsList, caseDefendant.get());
        } else {
            ejectExtract.withDefendant(transformDefendantWithoutHearingDetails(caseDefendant.get(), defendantBuilder));
            ejectExtract.withProsecutingAuthority(transformationHelper.transformProsecutingAuthority(hearingsAtAGlance.getProsecutionCaseIdentifier(), userId));
        }

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            ejectExtract.withCourtApplications(transformCourtApplications(hearingsAtAGlance.getCourtApplications(), hearingsList));
            ejectExtract.withIsAppealPending(transformationHelper.getAppealPendingFlag(hearingsAtAGlance.getCourtApplications()));
        }

        return ejectExtract.build();
    }

    private void extractHearingDetails(final GetHearingsAtAGlance hearingsAtAGlance, final UUID defendantId, final UUID userId, final CourtExtractRequested.Builder courtExtract,
                                       final Defendant.Builder defendantBuilder, final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Defendant caseDefendant) {
        final UUID masterDefendantId = caseDefendant.getMasterDefendantId();

        final Hearings latestHearing = hearingsList.size() > 1 ? transformationHelper.getLatestHearings(hearingsList) : hearingsList.get(0);

        courtExtract.withDefendant(transformDefendants(latestHearing.getDefendants(), defendantId, masterDefendantId, defendantBuilder, hearingsList, caseDefendant));

        courtExtract.withPublishingCourt(transformCourtCentre(latestHearing, userId, defendantId));

        courtExtract.withProsecutingAuthority(transformationHelper.transformProsecutingAuthority(hearingsAtAGlance.getProsecutionCaseIdentifier(), userId));

        courtExtract.withProsecutionCounsels(transformProsecutionCounsels(hearingsList));

        courtExtract.withCourtDecisions(transformCourtDecisions(hearingsList));

        courtExtract.withCompanyRepresentatives(transformCompanyRepresentatives(hearingsList));

        courtExtract.withReferralReason(getReferralReason(hearingsList));
    }

    private List<CourtApplications> transformCourtApplications(final List<CourtApplication> courtApplications, final List<Hearings> hearingsList) {
        final List<CourtApplication> applicationsExtractList = new ArrayList<>();

        courtApplications.forEach(app -> applicationsExtractList.add(mergeApplicationResults(app, getResultedApplication(app.getId(), hearingsList))));

        return applicationsExtractList.stream()
                .map(ca -> CourtApplications.courtApplications()
                        .withResults(ca.getJudicialResults())
                        .withRepresentation(transformRepresentation(ca, hearingsList))
                        .withApplicationType(ca.getType().getType())
                        .build()).collect(Collectors.toList());
    }

    private Optional<uk.gov.justice.progression.courts.CourtApplications> getResultedApplication(final UUID applicationId, final List<Hearings> hearingsList) {
        final Optional<uk.gov.justice.progression.courts.CourtApplications> resultedApplication = hearingsList.stream().filter(Objects::nonNull)
                .filter(h -> nonNull(h.getDefendants()))
                .flatMap(h -> h.getDefendants().stream()).filter(Objects::nonNull)
                .filter(d -> nonNull(d.getCourtApplications()))
                .flatMap(d -> d.getCourtApplications().stream())
                .filter(Objects::nonNull)
                .filter(ra -> applicationId.toString().equals(ra.getApplicationId().toString())).findFirst();
        LOGGER.info("Resulted application for {} is ", resultedApplication.isPresent() ? "found" : "not found");
        return resultedApplication;
    }

    private CourtApplication mergeApplicationResults(final CourtApplication courtApplication, final Optional<uk.gov.justice.progression.courts.CourtApplications> resultedApplication) {
        return (CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withType(courtApplication.getType())
                .withApplicant(courtApplication.getApplicant())
                .withRespondents(updateResponse(courtApplication.getRespondents(), resultedApplication.orElse(null)))
                .withJudicialResults(resultedApplication.map(uk.gov.justice.progression.courts.CourtApplications::getJudicialResults).orElse(null))
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .build());

    }

    private List<CourtApplicationParty> updateResponse(final List<CourtApplicationParty> courtApplicationRespondents, final uk.gov.justice.progression.courts.CourtApplications resultedApplication) {
        final List<CourtApplicationParty> updatedResponseList = new ArrayList<>();
        if (isNotEmpty(courtApplicationRespondents)) {
            updatedResponseList.addAll(courtApplicationRespondents);
        }
        if (nonNull(resultedApplication) && isNotEmpty(resultedApplication.getRespondents())) {
            // GPE-15039 Commented temporarily
//            final String responseDescription = resultedApplication.getRespondents().stream()
//                    .filter(Objects::nonNull)
//                    .filter(r -> nonNull(r.getApplicationResponse()))
//                    .map(Respondents::getApplicationResponse)
//                    .filter(Objects::nonNull)
//                    .findAny()
//                    .orElse(EMPTY);

//            final LocalDate responseDate = resultedApplication.getRespondents().stream()
//                    .filter(Objects::nonNull)
//                    .filter(r -> nonNull(r.getApplicationResponse()))
//                    .map(Respondents::getResponseDate)
//                    .filter(Objects::nonNull)
//                    .findAny()
//                    .orElse(null);

            updatedResponseList.add(CourtApplicationParty.courtApplicationParty()
                    // GPE-15039 Commented temporarily
//                    .withApplicationResponse(CourtApplicationResponse.courtApplicationResponse()
//                            .withApplicationResponseType(CourtApplicationResponseType.courtApplicationResponseType()
//                                    .withDescription(responseDescription).build())
//                            .withApplicationResponseDate(nonNull(responseDate) ? responseDate : null)
//                            .build())
                    .build());
        }
        return updatedResponseList;
    }

    private Representation transformRepresentation(final CourtApplication ca, final List<Hearings> hearingsList) {
        return Representation.representation()
                .withApplicantRepresentation(transformApplicantRepresentation(ca.getApplicant().getRepresentationOrganisation(), hearingsList))
                .withRespondentRepresentation(ca.getRespondents() != null ? transformRespondantRepresentations(ca.getRespondents() , hearingsList) : null)
                .build();
    }

    private String getReferralReason(final List<Hearings> hearingsList) {
        return hearingsList.stream()
                .filter(h -> nonNull(h.getDefendantReferralReasons()))
                .flatMap((h -> h.getDefendantReferralReasons().stream()))
                .filter(Objects::nonNull)
                .findAny()
                .map(ReferralReason::getDescription)
                .orElse(null);
    }

    private List<RespondentRepresentation> transformRespondantRepresentations(final List<CourtApplicationParty> respondents, final List<Hearings> hearingsList) {
        final List<RespondentRepresentation> respondentRepresentations = new ArrayList<>();

        respondents.forEach(r -> respondentRepresentations.add(transformRespondantRepresentation(r, hearingsList)));
        return respondentRepresentations;
    }

    private RespondentRepresentation transformRespondantRepresentation(final CourtApplicationParty courtApplicationParty, final List<Hearings> hearingsList) {
        final Organisation representationOrganisation = nonNull(courtApplicationParty) ? courtApplicationParty.getRepresentationOrganisation() : null;
        return RespondentRepresentation.respondentRepresentation()
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withName(nonNull(representationOrganisation) ? representationOrganisation.getName() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withRespondentCounsels(nonNull(hearingsList) ? hearingsList.stream()
                        .filter(hearings -> hearings.getRespondentCounsels() != null)
                        .flatMap(hearings -> hearings.getRespondentCounsels().stream())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private ApplicantRepresentation transformApplicantRepresentation(final Organisation representationOrganisation, final List<Hearings> hearingsList) {
        final String orgName = nonNull(representationOrganisation) ? representationOrganisation.getName() : null;
        return ApplicantRepresentation.applicantRepresentation()
                .withName(orgName)
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withApplicantCounsels(nonNull(hearingsList) ? hearingsList.stream()
                        .filter(hearings -> hearings.getApplicantCounsels() != null)
                        .flatMap(hearings -> hearings.getApplicantCounsels().stream())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private ParentGuardian transformParentGuardian(final List<AssociatedPerson> associatedPersons) {
        final Optional<Person> person = associatedPersons.stream()
                .filter(associatedPerson -> nonNull(associatedPerson.getPerson()))
                .findFirst()
                .map(AssociatedPerson::getPerson);
        return person.map(person1 -> ParentGuardian.parentGuardian()
                .withName(transformationHelper.getName(person1.getFirstName(), person1.getMiddleName(), person1.getLastName()))
                .withAddress(person1.getAddress())
                .build()).orElse(null);
    }

    private List<CourtDecisions> transformCourtDecisions(final List<Hearings> hearings) {
        final List<CourtDecisions> courtDecisionList = new ArrayList<>();
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
                courtDecisionList.add(courtDecisions);
            }
        });
        return courtDecisionList;
    }

    private List<ProsecutionCounsels> transformProsecutionCounsel(final List<ProsecutionCounsel> prosecutionCounselsList) {
        return prosecutionCounselsList.stream().map(pc -> ProsecutionCounsels.prosecutionCounsels()
                .withName(transformationHelper.getName(pc.getFirstName(), pc.getMiddleName(), pc.getLastName()))
                .withAttendanceDays(transformAttendanceDays(pc.getAttendanceDays()))
                .withRole(pc.getStatus())
                .build()
        ).collect(Collectors.toList());
    }

    private List<DefenceCounsels> transformDefenceCounsel(final List<DefenceCounsel> defenceCounselList) {
        return defenceCounselList.stream().map(dc -> DefenceCounsels.defenceCounsels()
                .withName(transformationHelper.getName(dc.getFirstName(), dc.getMiddleName(), dc.getLastName()))
                .withAttendanceDays(dc.getAttendanceDays() != null ? transformAttendanceDays(dc.getAttendanceDays()) : new ArrayList<>())
                .withRole(dc.getStatus())
                .build()).collect(Collectors.toList());
    }

    private List<AttendanceDays> transformAttendanceDays(final List<LocalDate> attendanceDaysList) {
        return attendanceDaysList.stream().distinct().map(ad -> AttendanceDays.attendanceDays()
                .withDay(ad)
                .build()).collect(Collectors.toList());
    }

    private List<AttendanceDayAndType> transformAttendanceDayAndTypes(final List<AttendanceDayAndType> attendanceDaysList) {
        return attendanceDaysList.stream().distinct().map(ad -> AttendanceDayAndType.attendanceDayAndType()
                .withDay(ad.getDay())
                .withAttendanceType(ad.getAttendanceType())
                .build()).collect(Collectors.toList());
    }

    private PublishingCourt transformCourtCentre(final Hearings latestHearing, final UUID userId, final UUID defendantId) {
        if(hearingsDefendantIdBiPredicate.test(latestHearing, defendantId)) {
            LOGGER.info("Latest hearing youth Court Name {} " , latestHearing.getYouthCourt().getName());
            return PublishingCourt.publishingCourt()
                    .withName(latestHearing.getYouthCourt().getName())
                    .withWelshName(latestHearing.getYouthCourt().getWelshName())
                    .withAddress(transformationHelper.getCourtAddress(userId, latestHearing.getCourtCentre().getId()))
                    .build();
        } else {
            return PublishingCourt.publishingCourt()
                    .withName(latestHearing.getCourtCentre().getName())
                    .withWelshName(latestHearing.getCourtCentre().getWelshName())
                    .withAddress(transformationHelper.getCourtAddress(userId, latestHearing.getCourtCentre().getId()))
                    .build();
        }
    }

    private Defendant transformDefendants(final List<Defendants> defendantsList, final UUID defendantId, final UUID masterDefendantId, final Defendant.Builder defendantBuilder, final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Defendant caseDefendant) {
        final Optional<Defendants> defendants = defendantsList.stream().filter(d -> d.getId().equals(defendantId)).findFirst();
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
            defendantBuilder.withHearings(transformHearing(hearingsList, defendant.getId()));
            defendantBuilder.withAttendanceDays(transformAttendanceDayAndTypes(transformDefendantAttendanceDay(hearingsList, defendant)));
            defendantBuilder.withResults(transformJudicialResults(hearingsList, masterDefendantId, defendantId));
            final List<uk.gov.justice.progression.courts.exract.Offences> offences = transformOffence(hearingsList, defendantId.toString());
            if (!offences.isEmpty()) {
                defendantBuilder.withOffences(offences);
            }
            if (nonNull(caseDefendant.getAssociatedDefenceOrganisation())) {
                defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(hearingsList, caseDefendant.getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation(), defendantId.toString()));
            } else {
                if (nonNull(caseDefendant.getDefenceOrganisation())) {
                    defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(hearingsList, caseDefendant.getDefenceOrganisation(), defendantId.toString()));
                } else if (nonNull(defendant.getDefenceOrganisation())) {
                    defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(hearingsList, defendant.getDefenceOrganisation().getDefenceOrganisation(), defendantId.toString()));
                } else {
                    defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(hearingsList, null, defendantId.toString()));
                }
            }

            if (nonNull(caseDefendant.getPersonDefendant()) && nonNull(caseDefendant.getPersonDefendant().getArrestSummonsNumber())) {
                defendantBuilder.withArrestSummonsNumber(caseDefendant.getPersonDefendant().getArrestSummonsNumber());
            }
        }

        return defendantBuilder.build();
    }

    private List<JudicialResult> transformJudicialResults(final List<Hearings> hearingsList, final UUID masterDefendantId, final UUID defendantId) {
        final List<JudicialResult> judicialResultsList = new ArrayList<>();
        final List<JudicialResult> defendantLevelJudicialResults = hearingsList.stream()
                .map(Hearings::getDefendantJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(defendantJudicialResult -> masterDefendantId.equals(defendantJudicialResult.getMasterDefendantId()))
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(Objects::nonNull)
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .collect(toList());
        judicialResultsList.addAll(defendantLevelJudicialResults);

        final List<JudicialResult> caseLevelJudicialResults = hearingsList.stream().map(Hearings::getDefendants)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .map(Defendants::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(jr ->nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .collect(toList());

        judicialResultsList.addAll(caseLevelJudicialResults);

        return judicialResultsList.stream()
                .map(this::transformResult)
                .collect(toList());
    }

    private JudicialResult transformResult(final JudicialResult result) {
        // If result contains resultWording then set the resultText with resultWording.
        if (isNotEmpty(result.getResultWording())) {
            return judicialResult()
                    .withAlwaysPublished(result.getAlwaysPublished())
                    .withAmendmentDate(result.getAmendmentDate())
                    .withAmendmentReason(result.getAmendmentReason())
                    .withAmendmentReasonId(result.getAmendmentReasonId())
                    .withApprovedDate(result.getApprovedDate())
                    .withCategory(result.getCategory())
                    .withCjsCode(result.getCjsCode())
                    .withCourtClerk(result.getCourtClerk())
                    .withD20(result.getD20())
                    .withDelegatedPowers(result.getDelegatedPowers())
                    .withDurationElement(result.getDurationElement())
                    .withExcludedFromResults(result.getExcludedFromResults())
                    .withFourEyesApproval(result.getFourEyesApproval())
                    .withIsAdjournmentResult(result.getIsAdjournmentResult())
                    .withIsAvailableForCourtExtract(result.getIsAvailableForCourtExtract())
                    .withIsConvictedResult(result.getIsConvictedResult())
                    .withIsDeleted(result.getIsDeleted())
                    .withIsFinancialResult(result.getIsFinancialResult())
                    .withJudicialResultId(result.getJudicialResultId())
                    .withJudicialResultPrompts(filterOutPromptsNotToBeShownInCourtExtract(result))
                    .withJudicialResultTypeId(result.getJudicialResultTypeId())
                    .withLabel(result.getLabel())
                    .withLastSharedDateTime(result.getLastSharedDateTime())
                    .withLifeDuration(result.getLifeDuration())
                    .withNextHearing(result.getNextHearing())
                    .withOrderedDate(result.getOrderedDate())
                    .withOrderedHearingId(result.getOrderedHearingId())
                    .withPostHearingCustodyStatus(result.getPostHearingCustodyStatus())
                    .withPublishedForNows(result.getPublishedForNows())
                    .withPublishedAsAPrompt(result.getPublishedAsAPrompt())
                    .withQualifier(result.getQualifier())
                    .withRank(result.getRank())
                    .withResultDefinitionGroup(result.getResultDefinitionGroup())
                    .withResultText(result.getResultText())
                    .withResultWording(result.getResultWording())
                    .withRollUpPrompts(result.getRollUpPrompts())
                    .withTerminatesOffenceProceedings(result.getTerminatesOffenceProceedings())
                    .withUrgent(result.getUrgent())
                    .withUsergroups(result.getUsergroups())
                    .withWelshLabel(result.getWelshLabel())
                    .withWelshResultWording(result.getWelshResultWording())
                    .withPoliceSubjectLineTitle(result.getPoliceSubjectLineTitle())
                    .build();
        }
        return judicialResult()
                .withValuesFrom(result)
                .withJudicialResultPrompts(filterOutPromptsNotToBeShownInCourtExtract(result))
                .build();
    }

    private List<JudicialResultPrompt> filterOutPromptsNotToBeShownInCourtExtract(final JudicialResult result) {
        return isNotEmpty(result.getJudicialResultPrompts()) ? result.getJudicialResultPrompts().stream().filter(jrp -> "Y".equals(jrp.getCourtExtract())).collect(toList()) : result.getJudicialResultPrompts();
    }

    private Defendant transformDefendantWithoutHearingDetails(final uk.gov.justice.core.courts.Defendant caseDefendant, final Defendant.Builder defendantBuilder) {
        if (nonNull(caseDefendant)) {
            if(nonNull(caseDefendant.getPersonDefendant())) {
                final Person personDefendant = caseDefendant.getPersonDefendant().getPersonDetails();
                defendantBuilder.withDateOfBirth(personDefendant.getDateOfBirth());
                defendantBuilder.withAge(transformationHelper.getDefendantAge(caseDefendant));
                defendantBuilder.withAddress(personDefendant.getAddress());
            }

            if(nonNull(caseDefendant.getLegalEntityDefendant()) && nonNull(caseDefendant.getLegalEntityDefendant().getOrganisation().getAddress())) {
                defendantBuilder.withAddress(caseDefendant.getLegalEntityDefendant().getOrganisation().getAddress());
            }
        }
        defendantBuilder.withOffences(transformDefendantOffences(caseDefendant.getOffences()));
        defendantBuilder.withDefenceOrganisations(nonNull(caseDefendant.getDefenceOrganisation()) ? getDefenceOrganisation(caseDefendant) : null);
        return defendantBuilder.build();
    }

    private List<DefenceOrganisations> transformDefenceOrganisation(final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Organisation organisation, final String defendantId) {
        return Collections.singletonList(
                DefenceOrganisations.defenceOrganisations()
                        .withDefenceCounsels(isNotEmpty(hearingsList) ? transformDefenceCounsels(hearingsList, defendantId) : null)
                        .withDefenceOrganisation(organisation)
                        .build()
        );
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final List<Hearings> hearingsList, final String defendantId) {
        final List<uk.gov.justice.progression.courts.exract.Offences> offences = new ArrayList<>();

        final List<Offences> defendantOffences = hearingsList.stream().map(Hearings::getDefendants)
                .flatMap(d -> d.stream()).filter(Objects::nonNull).filter(d -> d.getId().toString().equals(defendantId))
                .map(Defendants::getOffences).flatMap(Collection::stream).collect(Collectors.toList());

        if (isNotEmpty(defendantOffences)) {
            LOGGER.info("Defendant {} aggregated offences: {}", defendantId, defendantOffences.size());
            final List<UUID> hearingIds = hearingsList.stream().map(Hearings::getId).collect(toList());
            offences.addAll(transformOffence(defendantOffences, hearingIds));
        }
        return offences;
    }

    private List<AttendanceDayAndType> transformDefendantAttendanceDay(final List<Hearings> hearingsList, final Defendants defendant) {

        return hearingsList.stream()
                .filter(h -> nonNull(h.getDefendantAttendance()))
                .flatMap((h -> h.getDefendantAttendance().stream()))
                .filter(Objects::nonNull)
                .filter(da -> da.getDefendantId().toString().equals(defendant.getId().toString()))
                .map(DefendantAttendance::getAttendanceDays)
                .flatMap(Collection::stream)
                .filter(ad -> !ad.getAttendanceType().equals(AttendanceType.NOT_PRESENT))
                .map(ad -> AttendanceDayAndType.attendanceDayAndType()
                        .withDay(ad.getDay())
                        .withAttendanceType(ad.getAttendanceType().toString().equals(AttendanceType.IN_PERSON.toString()) ? PRESENT_IN_PERSON : extractAttendanceType(defendant)).build())
                .collect(Collectors.toList());

    }

    private String extractAttendanceType(final Defendants defendant) {
        if (nonNull(defendant.getCustodialEstablishment())) {
            final String custody = defendant.getCustodialEstablishment().getCustody();
            if ("Prison".equals(custody)) {
                return PRESENT_BY_PRISON_VIDEO_LINK;
            } else if ("Police".equals(custody)) {
                return PRESENT_BY_POLICE_VIDEO_LINK;
            }
        }
        return PRESENT_BY_VIDEO_DEFAULT;
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final List<Offences> offences, final List<UUID> hearingIds) {

        return offences.stream().map(o -> uk.gov.justice.progression.courts.exract.Offences.offences()
                .withId(o.getId())
                .withConvictionDate(o.getConvictionDate())
                .withCount(o.getCount())
                .withEndDate(o.getEndDate())
                .withIndicatedPlea(o.getIndicatedPlea())
                .withAllocationDecision(o.getAllocationDecision())
                .withStartDate(o.getStartDate())
                .withOffenceDefinitionId(o.getOffenceDefinitionId())
                .withOffenceLegislation(o.getOffenceLegislation())
                .withOffenceLegislationWelsh(o.getOffenceLegislationWelsh())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceTitle(o.getOffenceTitle())
                .withOffenceTitleWelsh(o.getOffenceTitleWelsh())
                .withResults(transformResults(filterOutResultDefinitionsNotToBeShownInCourtExtract(o, hearingIds)))
                .withNotifiedPlea(o.getNotifiedPlea())
                .withWording(o.getWording())
                .withPleas(o.getPleas())
                .withVerdicts(o.getVerdicts())
                .withWordingWelsh(o.getWordingWelsh()).build()

        ).collect(toList());
    }

    private List<JudicialResult> filterOutResultDefinitionsNotToBeShownInCourtExtract(final Offences o, final List<UUID> hearingIds) {
        return isNotEmpty(o.getJudicialResults()) ?
                o.getJudicialResults().stream()
                        .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract() &&
                                hearingIds.contains(jr.getOrderedHearingId()))
                        .collect(toList())
                : o.getJudicialResults();
    }

    private List<JudicialResult> transformResults(final List<JudicialResult> judicialResults) {
        if (isNotEmpty(judicialResults)) {
            return judicialResults.stream()
                    .map(this::transformResult)
                    .collect(toList());
        }
        return judicialResults;
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> transformDefendantOffences(final List<Offence> offences) {

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
                .withPleas(getOffencePleas(o.getPlea()))
                .withVerdicts(getOffenceVerdicts(o.getVerdict()))
                .withWordingWelsh(o.getWordingWelsh()).build()

        ).collect(Collectors.toList());
    }

    private List<uk.gov.justice.progression.courts.exract.Hearings> transformHearing(final List<Hearings> hearingsList, final UUID defendantId) {
        return hearingsList.stream().map(h ->
                uk.gov.justice.progression.courts.exract.Hearings.hearings()
                        .withHearingDays(transformationHelper.transformHearingDays(h.getHearingDays()))
                        .withId(h.getId())
                        .withJurisdictionType(h.getJurisdictionType() != null ? JurisdictionType.valueOf(h.getJurisdictionType().toString()) : null)
                        .withCourtCentre(transformCourtCenter(h, defendantId))
                        .withReportingRestrictionReason(h.getReportingRestrictionReason())
                        .withType(h.getType().getDescription()).build()
        ).collect(Collectors.toList());
    }

    private uk.gov.justice.progression.courts.exract.CourtCentre transformCourtCenter(final Hearings hearings, final UUID defendantId) {
        if(hearingsDefendantIdBiPredicate.test(hearings,defendantId)) {
            LOGGER.info("hearings Youth Court Name {}" , hearings.getYouthCourt().getName());
            return  uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                    .withName(hearings.getYouthCourt().getName())
                    .withWelshName(hearings.getYouthCourt().getWelshName())
                    .withId(hearings.getYouthCourt().getYouthCourtId())
                    .build();
        }
        return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                .withName(hearings.getCourtCentre().getName())
                .withId(hearings.getCourtCentre().getId())
                .withWelshName(hearings.getCourtCentre().getWelshName())
                .build();
    }

    private List<CompanyRepresentatives> transformCompanyRepresentative(final List<CompanyRepresentative> companyRepresentatives) {
        return companyRepresentatives.stream().map(cr -> CompanyRepresentatives.companyRepresentatives()
                .withName(transformationHelper.getName(cr.getFirstName(), EMPTY, cr.getLastName()))
                .withAttendanceDays(transformAttendanceDays(cr.getAttendanceDays()))
                .withRole(cr.getPosition() != null ? cr.getPosition().toString() : EMPTY)
                .build()
        ).collect(Collectors.toList());
    }

    private List<ProsecutionCounsels> transformProsecutionCounsels(final List<Hearings> hearingsList) {
        return hearingsList.stream()
                .filter(hearing -> isNotEmpty(hearing.getProsecutionCounsels()))
                .map(hearing -> transformProsecutionCounsel(hearing.getProsecutionCounsels()))
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<DefenceCounsels> transformDefenceCounsels(final List<Hearings> hearingsList, final String defendantId) {
        return hearingsList.stream()
                .filter(hearing -> isNotEmpty(hearing.getDefendants()))
                .flatMap((h -> h.getDefendants().stream()))
                .filter(da -> da.getId().toString().equals(defendantId))
                .map(da -> transformDefenceCounsel(da.getDefenceOrganisation().getDefenceCounsels()))
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<CompanyRepresentatives> transformCompanyRepresentatives(final List<Hearings> hearingsList) {
        return hearingsList.stream()
                .filter(hearing -> isNotEmpty(hearing.getCompanyRepresentatives()))
                .map(hearing -> transformCompanyRepresentative(hearing.getCompanyRepresentatives()))
                .flatMap(Collection::stream).collect(Collectors.toList());
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

    private static List<DefenceOrganisations> getDefenceOrganisation(final uk.gov.justice.core.courts.Defendant defendant) {
        return Collections.singletonList(DefenceOrganisations.defenceOrganisations()
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .build());
    }

}
